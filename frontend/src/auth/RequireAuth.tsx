import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import type { Role } from '../api/types'
import { Loader } from '../components/ui'
import { homeOf, useAuth } from './AuthProvider'

/**
 * Page reservee aux utilisateurs connectes, eventuellement a certains roles.
 * Un utilisateur sans le bon role retourne a son propre espace (gestion ou « Mon espace »).
 */
export function RequireAuth({ roles, children }: { roles?: Role[]; children: ReactNode }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <Loader />
  // Les pages de gestion renvoient vers la connexion de l'equipe, les autres vers celle des clients
  const staffOnly = roles !== undefined && !roles.includes('CLIENT')
  if (!user) return <Navigate to={staffOnly ? '/espace-equipe' : '/connexion'} replace state={{ from: location.pathname }} />
  if (roles && !roles.includes(user.role)) return <Navigate to={homeOf(user)} replace />
  return children
}
