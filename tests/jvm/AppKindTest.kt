import com.willykez.campconnect.data.model.AppKind
import com.willykez.campconnect.data.model.AccountStatus
import com.willykez.campconnect.data.model.Role
import com.willykez.campconnect.data.model.UserProfile
import com.willykez.campconnect.viewmodel.ScreenTab

var pass = 0; var fail = 0
fun check(n: String, ok: Boolean) { if (ok) pass++ else { fail++; println("FAIL $n") } }

fun profile(role: Role, platform: Boolean = false) = UserProfile(
  id = "u", fullName = "Test Person", email = "t@x.co", role = role, status = AccountStatus.APPROVED, universityId = "cuom", isPlatformAdmin = platform
)

fun main() {
  val student = profile(Role.STUDENT); val lecturer = profile(Role.LECTURER)
  val admin = profile(Role.ADMIN); val owner = profile(Role.ADMIN, platform = true)

  val accepts = mapOf(
    AppKind.STUDENT to listOf(true, false, false, false),
    AppKind.LECTURER to listOf(false, true, false, false),
    AppKind.ADMIN to listOf(false, false, true, false),
    AppKind.OWNER to listOf(false, false, false, true)
  )
  for ((kind, expected) in accepts) {
    val actual = listOf(student, lecturer, admin, owner).map { kind.accepts(it) }
    check("${kind.name} accepts exactly its own account type", actual == expected)
  }
  check("every account type is accepted by exactly one app", listOf(student, lecturer, admin, owner).all { p -> AppKind.values().count { it.accepts(p) } == 1 })
  check("of() points each account at the app that accepts it", listOf(student, lecturer, admin, owner).all { AppKind.of(it).accepts(it) })
  check("the app manager is the owner app's, never the admin app's", AppKind.of(owner) == AppKind.OWNER && !AppKind.ADMIN.accepts(owner))
  check("package names are the four requested ids", AppKind.values().map { it.packageName }.toSet() == setOf(
    "com.willykez.campconnect.owner", "com.willykez.campconnect.admin", "com.willykez.campconnect.lecturer", "com.willykez.campconnect.student"))
  check("only student and lecturer apps allow self-registration", AppKind.values().filter { it.canRegister }.toSet() == setOf(AppKind.STUDENT, AppKind.LECTURER))
  check("registration is fixed per app: lecturer app registers staff, student app students", AppKind.LECTURER.registersStaff && !AppKind.STUDENT.registersStaff && !AppKind.ADMIN.registersStaff)
  check("admin app has the Admin tab, others do not", ScreenTab.ADMIN in AppKind.ADMIN.tabs && AppKind.values().filter { it != AppKind.ADMIN }.none { ScreenTab.ADMIN in it.tabs })
  check("owner app has no bottom tabs (it is the platform screen)", AppKind.OWNER.tabs.isEmpty())
  check("every tab list starts with Home and ends with Profile", AppKind.values().filter { it.tabs.isNotEmpty() }.all { it.tabs.first() == ScreenTab.HOME && it.tabs.last() == ScreenTab.PROFILE })
  check("tab lists have no duplicates and at most 5 entries (bottom bar limit)", AppKind.values().all { it.tabs.distinct().size == it.tabs.size && it.tabs.size <= 5 })
  check("student and lecturer apps cannot reach the admin tab", ScreenTab.ADMIN !in AppKind.STUDENT.tabs && ScreenTab.ADMIN !in AppKind.LECTURER.tabs)
  println("AppKind: $pass passed, $fail failed"); System.exit(if (fail == 0) 0 else 1)
}
