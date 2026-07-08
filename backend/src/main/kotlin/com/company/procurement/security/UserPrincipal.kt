package com.company.procurement.security

import com.company.procurement.model.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserPrincipal(
    val id: String,
    private val email: String,
    private val password: String,
    val role: String,
    val active: Boolean,
    val firstName: String,
    val lastName: String
) : UserDetails {

    companion object {
        fun fromUser(user: User): UserPrincipal {
            return UserPrincipal(
                id = user.id ?: "",
                email = user.email,
                password = user.password,
                role = user.role.name,
                active = user.active,
                firstName = user.firstName,
                lastName = user.lastName
            )
        }
    }

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_$role"))
    }

    override fun getPassword(): String = password

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = active
}
