import { Flower2 } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { errorMessage, fieldErrors } from '../../api/client'
import { homeOf, useAuth } from '../../auth/AuthProvider'
import { Alert, Button, Field } from '../../components/ui'

// US-01 : connexion de l'equipe de FloreEvent (gerant, commerciaux, logistique)
export default function StaffLoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
  // Page demandee avant la connexion ; sinon l'accueil de la gestion
  const from = (useLocation().state as { from?: string } | null)?.from
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [sending, setSending] = useState(false)

  if (user) return <Navigate to={from ?? homeOf(user)} replace />

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setSending(true)
    setError('')
    setErrors({})
    try {
      const connected = await login(email, password)
      navigate(from ?? homeOf(connected), { replace: true })
    } catch (err) {
      setErrors(fieldErrors(err))
      setError(errorMessage(err))
      setSending(false)
    }
  }

  return (
    <main className="flex min-h-dvh items-center justify-center px-4 py-10">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center text-center">
          <span className="flex size-12 items-center justify-center rounded-2xl bg-brand-700 text-white">
            <Flower2 className="size-6" aria-hidden />
          </span>
          <h1 className="mt-4 text-2xl font-extrabold tracking-tight">Espace équipe</h1>
          <p className="mt-1 text-sm text-slate-600">Réservé à l’équipe de FloreEvent.</p>
        </div>
        <form onSubmit={submit} className="space-y-4 rounded-2xl bg-white p-6 ring-1 ring-stone-200" noValidate>
          <Alert>{error}</Alert>
          <Field label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} required />
          <Field
            label="Mot de passe"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={errors.password}
            required
          />
          <Button type="submit" loading={sending} className="w-full">
            Se connecter
          </Button>
        </form>
        <p className="mt-4 text-center text-sm">
          <Link to="/mot-de-passe-oublie" className="font-semibold text-brand-700">
            Mot de passe oublié ?
          </Link>
        </p>
        <p className="mt-6 text-center text-xs text-slate-500">Chaque membre de l’équipe a son propre compte, créé par le gérant qui lui donne ses droits.</p>
        <p className="mt-4 text-center text-sm">
          <Link to="/" className="font-semibold text-slate-600 hover:text-brand-700">
            ← Retour au site
          </Link>
        </p>
      </div>
    </main>
  )
}
