import { useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { authApi, tokenStore } from '../api/client'
import { STAFF_ROLES } from '../api/types'
import type { RegisterForm, Role, User } from '../api/types'

type AuthContextValue = {
  user: User | null
  loading: boolean
  login: (email: string, password: string) => Promise<User>
  register: (form: RegisterForm) => Promise<User>
  logout: () => void
  /** Met a jour l'utilisateur affiche (nom, photo) apres une modification du profil */
  setUser: (user: User) => void
  hasRole: (...roles: Role[]) => boolean
  isStaff: boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

/** Page d'arrivee apres la connexion : l'espace de gestion pour l'equipe, « Mon espace » pour les clients. */
// eslint-disable-next-line react-refresh/only-export-components
export const homeOf = (user: User) => (STAFF_ROLES.includes(user.role) ? '/gestion' : '/mon-espace')

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<User | null>(null)
  // Vrai tant que le jeton enregistre n'a pas ete verifie aupres du serveur
  const [loading, setLoading] = useState(() => Boolean(tokenStore.get()))

  const logout = useCallback(() => {
    tokenStore.clear()
    setUser(null)
    queryClient.clear()
  }, [queryClient])

  useEffect(() => {
    window.addEventListener('auth:logout', logout)
    if (tokenStore.get()) {
      authApi
        .me()
        .then(setUser)
        .catch(logout)
        .finally(() => setLoading(false))
    }
    return () => window.removeEventListener('auth:logout', logout)
  }, [logout])

  const openSession = useCallback((session: { token: string; user: User }) => {
    tokenStore.set(session.token)
    setUser(session.user)
    return session.user
  }, [])

  const login = useCallback(
    async (email: string, password: string) => openSession(await authApi.login(email, password)),
    [openSession],
  )

  const register = useCallback(async (form: RegisterForm) => openSession(await authApi.register(form)), [openSession])

  const hasRole = useCallback((...roles: Role[]) => Boolean(user && roles.includes(user.role)), [user])
  const isStaff = Boolean(user && STAFF_ROLES.includes(user.role))

  const value = useMemo(
    () => ({ user, loading, login, register, logout, setUser, hasRole, isStaff }),
    [user, loading, login, register, logout, hasRole, isStaff],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth doit être utilisé dans AuthProvider')
  return context
}
