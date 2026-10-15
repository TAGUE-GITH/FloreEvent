import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { errorMessage, fieldErrors } from '../../api/client'
import type { RegisterForm } from '../../api/types'
import { homeOf, useAuth } from '../../auth/AuthProvider'
import { Alert, Button, Card, Checkbox, Field } from '../../components/ui'

// US-06 : creation d'un compte client, avec acceptation des conditions generales (RGPD)
export default function RegisterPage() {
  const { user, register } = useAuth()
  const navigate = useNavigate()
  // Venu du devis : on y retourne pour envoyer la demande
  const from = (useLocation().state as { from?: string } | null)?.from
  const [form, setForm] = useState<RegisterForm>({ firstName: '', lastName: '', email: '', phone: '', company: '', password: '', acceptTerms: false })
  const [error, setError] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [sending, setSending] = useState(false)

  if (user && !sending) return <Navigate to={homeOf(user)} replace />
  const set = (changes: Partial<RegisterForm>) => setForm((current) => ({ ...current, ...changes }))

  const submit = async (event: FormEvent) => {
    event.preventDefault()
    setSending(true)
    setError('')
    setErrors({})
    try {
      await register(form)
      navigate(from ?? '/mon-espace', { replace: true, state: { welcome: true } })
    } catch (err) {
      const byField = fieldErrors(err)
      setErrors(byField)
      setError(Object.keys(byField).length ? '' : errorMessage(err))
      setSending(false)
    }
  }

  return (
    <div className="mx-auto max-w-lg px-4 py-12">
      <h1 className="text-3xl font-extrabold tracking-tight">Créer mon compte</h1>
      <p className="mt-2 text-slate-600">Envoyez vos demandes de devis et suivez vos réservations.</p>
      <Card className="mt-6">
        <form onSubmit={submit} className="space-y-4" noValidate>
          <Alert>{error}</Alert>
          <div className="grid grid-cols-2 gap-3">
            <Field label="Prénom" value={form.firstName} onChange={(e) => set({ firstName: e.target.value })} error={errors.firstName} autoComplete="given-name" />
            <Field label="Nom" value={form.lastName} onChange={(e) => set({ lastName: e.target.value })} error={errors.lastName} autoComplete="family-name" />
          </div>
          <Field label="Email" type="email" value={form.email} onChange={(e) => set({ email: e.target.value })} error={errors.email} autoComplete="email" />
          <Field label="Téléphone" type="tel" value={form.phone} onChange={(e) => set({ phone: e.target.value })} error={errors.phone} autoComplete="tel" placeholder="06 12 34 56 78" />
          <Field label="Société ou association (facultatif)" value={form.company} onChange={(e) => set({ company: e.target.value })} error={errors.company} autoComplete="organization" />
          <Field label="Mot de passe" type="password" value={form.password} onChange={(e) => set({ password: e.target.value })} error={errors.password} autoComplete="new-password" hint="8 caractères minimum" />
          <Checkbox
            checked={form.acceptTerms}
            onChange={(e) => set({ acceptTerms: e.target.checked })}
            error={errors.acceptTerms}
            label={
              <>
                J’ai lu et j’accepte les{' '}
                <Link to="/conditions-generales" target="_blank" className="font-semibold text-brand-700 underline">
                  conditions générales
                </Link>{' '}
                et la{' '}
                <Link to="/confidentialite" target="_blank" className="font-semibold text-brand-700 underline">
                  politique de confidentialité
                </Link>
                .
              </>
            }
          />
          <Button type="submit" loading={sending} className="w-full">
            Créer mon compte
          </Button>
        </form>
      </Card>
      <p className="mt-6 text-center text-sm text-slate-600">
        Déjà un compte ?{' '}
        <Link to="/connexion" state={from ? { from } : undefined} className="font-semibold text-brand-700">
          Se connecter
        </Link>
      </p>
    </div>
  )
}
