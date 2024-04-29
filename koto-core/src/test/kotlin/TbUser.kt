import com.kotoframework.annotations.Column
import com.kotoframework.interfaces.KPojo
import java.util.*

/**
 * Created by ousc on 2022/4/18 17:54
 */
data class TbUser(
    val id: Int? = null,
    val userName: String? = null,
    val password: String? = null,
    val nickname: String? = null,
    @Column("phone_number") val telephone: String? = null,
    @Column("email_address") val email: String? = null,
    @Column("birthday") val birthday: String? = null,
    val sex: String? = null,
    val age: Int? = null,
    val avatar: String? = null,
    val updateTime: Date? = null,
) : KPojo