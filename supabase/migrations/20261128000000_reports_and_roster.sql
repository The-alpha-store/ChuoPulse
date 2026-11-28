-- Reports, the lecturer's student list and the sex field.
-- * profiles.sex: 'female' or 'male' (optional). Set by the person, or by their campus administrator.
-- * report_people(): ONE place that decides who may see which people in a report.
--     campus administrator -> every student and lecturer of their own campus
--     lecturer             -> only the students of the programmes / courses they teach
--     anyone else          -> nothing
--   It never returns passwords, tokens or other campuses' people.

alter table public.profiles add column if not exists sex text;
alter table public.profiles drop constraint if exists profiles_sex_check;
alter table public.profiles add constraint profiles_sex_check check (sex is null or sex in ('female', 'male'));

-- ---------- The people a report may list ----------------------------------------------------
create or replace function public.report_people()
returns table (
  person_id uuid, person_name text, person_email text, person_role text, person_status text,
  person_number text, person_sex text, person_programme text, person_department text,
  person_cohort text, person_courses text
)
language plpgsql stable security definer set search_path = public as $$
begin
  if public.is_admin() and not public.is_platform_admin() then
    return query
    select p.id, p.full_name, p.email, p.role::text, p.status::text,
           coalesce(case when p.role = 'student' then p.student_number else p.staff_number end, p.student_number, p.staff_number, ''),
           p.sex, coalesce(g.name, ''), coalesce(p.department, ''), coalesce(p.cohort, ''),
           coalesce((select string_agg(public.short_code(c.course_code), ', ' order by public.short_code(c.course_code))
                       from (select e.course_code from public.enrollments e where e.student_id = p.id and p.role = 'student'
                             union all
                             select t.course_code from public.teaching_assignments t where t.lecturer_id = p.id and p.role = 'lecturer') c), '')
    from public.profiles p
    left join public.programmes g on g.id = p.programme_id
    where p.university_id = public.my_university()
      and p.role in ('student', 'lecturer') and p.status = 'approved'
    order by p.full_name;
  elsif public.is_lecturer() then
    return query
    select p.id, p.full_name, p.email, p.role::text, p.status::text,
           coalesce(p.student_number, ''), p.sex, coalesce(g.name, ''), coalesce(p.department, ''), coalesce(p.cohort, ''),
           coalesce((select string_agg(public.short_code(e.course_code), ', ' order by public.short_code(e.course_code))
                       from public.enrollments e
                      where e.student_id = p.id
                        and exists (select 1 from public.teaching_assignments t
                                     where t.lecturer_id = auth.uid() and t.course_code = e.course_code)), '')
    from public.profiles p
    left join public.programmes g on g.id = p.programme_id
    where p.university_id = public.my_university()
      and p.role = 'student' and p.status = 'approved'
      and (exists (select 1 from public.enrollments e
                    join public.teaching_assignments t on t.course_code = e.course_code
                   where e.student_id = p.id and t.lecturer_id = auth.uid())
           or (p.programme_id is not null and public.teaches_programme(p.programme_id)))
    order by p.full_name;
  end if;
end $$;
revoke all on function public.report_people() from public, anon;
grant execute on function public.report_people() to authenticated;

-- ---------- Sex: yourself, or (administrator) someone on your campus -------------------------
create or replace function public.set_my_sex(p_sex text)
returns void language plpgsql security definer set search_path = public as $$
begin
  if auth.uid() is null then raise exception 'not_allowed' using errcode = '42501'; end if;
  if p_sex is not null and p_sex not in ('female', 'male') then raise exception 'invalid_sex' using errcode = 'P0001'; end if;
  perform set_config('app.internal', '1', true);
  update public.profiles set sex = p_sex where id = auth.uid();
end $$;
revoke all on function public.set_my_sex(text) from public, anon;
grant execute on function public.set_my_sex(text) to authenticated;

create or replace function public.admin_set_sex(p_user uuid, p_sex text)
returns void language plpgsql security definer set search_path = public as $$
begin
  if not public.admin_of(p_user) or public.is_platform_admin() then raise exception 'not_allowed' using errcode = '42501'; end if;
  if p_sex is not null and p_sex not in ('female', 'male') then raise exception 'invalid_sex' using errcode = 'P0001'; end if;
  perform set_config('app.internal', '1', true);
  update public.profiles set sex = p_sex where id = p_user;
end $$;
revoke all on function public.admin_set_sex(uuid, text) from public, anon;
grant execute on function public.admin_set_sex(uuid, text) to authenticated;

-- Used right after a bulk add: the administrator knows the email, not the new id.
create or replace function public.admin_set_sex_by_email(p_email text, p_sex text)
returns void language plpgsql security definer set search_path = public as $$
declare v_id uuid;
begin
  select id into v_id from public.profiles where lower(email) = lower(trim(p_email));
  if v_id is null then return; end if;
  perform public.admin_set_sex(v_id, p_sex);
end $$;
revoke all on function public.admin_set_sex_by_email(text, text) from public, anon;
grant execute on function public.admin_set_sex_by_email(text, text) to authenticated;
