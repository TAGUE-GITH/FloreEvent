import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { errorMessage, fieldErrors } from '../../api/client'
import { homeOf, useAuth } from '../../auth/AuthProvider'
import { Alert, Button, Card, Field } from '../../components/ui'

// Connexion des clients du site (l'equipe se connecte sur /espace-equipe)
export default function ClientLoginPage() {
  const { user, login } = useAuth()
  const navigate = useNavigate()
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
    <div className="mx-auto max-w-md px-4 py-12">
      <h1 className="text-3xl font-extrabold tracking-tight">Connexion</h1>
      <p className="mt-2 text-slate-600">Retrouvez vos demandes de devis et vos réservations.</p>
      <Card className="mt-6">
        <form onSubmit={submit} className="space-y-4" noValidate>
          <Alert>{error}</Alert>
          <Field label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} />
          <Field label="Mot de passe" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} error={errors.password} />
          <Button type="submit" loading={sending} className="w-full">
            Se connecter
          </Button>
        </form>
      </Card>
      <p className="mt-6 text-center text-sm text-slate-600">
        Pas encore de compte ?{' '}
        <Link to="/inscription" state={from ? { from } : undefined} className="font-semibold text-brand-700">
          Créer un compte
        </Link>
      </p>
    </div>
  )
}
