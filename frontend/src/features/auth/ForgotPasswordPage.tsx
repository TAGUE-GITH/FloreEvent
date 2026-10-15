import { useMutation } from '@tanstack/react-query'
import { KeyRound, MailCheck } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { authApi, errorMessage, fieldErrors } from '../../api/client'
import { Alert, Button, Field } from '../../components/ui'

// Mot de passe oublie : l'utilisateur recoit un lien par email pour en choisir un nouveau
export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const send = useMutation({ mutationFn: () => authApi.forgotPassword(email.trim()) })
  const errors = fieldErrors(send.error)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    send.mutate()
  }

  return (
    <div className="flex justify-center px-4 py-14">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center text-center">
          <span className="flex size-12 items-center justify-center rounded-2xl bg-brand-700 text-white">
            <KeyRound className="size-6" aria-hidden />
          </span>
          <h1 className="mt-4 text-2xl font-extrabold tracking-tight">Mot de passe oublié</h1>
          <p className="mt-1 text-sm text-slate-600">Indiquez l’email de votre compte : nous vous envoyons un lien pour en choisir un nouveau.</p>
        </div>
        {send.isSuccess ? (
          <div className="rounded-2xl bg-white p-6 text-center ring-1 ring-stone-200">
            <MailCheck className="mx-auto size-10 text-emerald-600" aria-hidden />
            <p className="mt-3 font-semibold">Vérifiez votre boîte email</p>
            <p className="mt-1 text-sm text-slate-600">
              Si un compte existe pour <strong>{email.trim()}</strong>, un email vient de lui être envoyé. Le lien est valable 1 heure. Pensez à regarder dans les courriers indésirables.
            </p>
          </div>
        ) : (
          <form onSubmit={submit} className="space-y-4 rounded-2xl bg-white p-6 ring-1 ring-stone-200" noValidate>
            <Alert>{send.isError && !Object.keys(errors).length ? errorMessage(send.error) : ''}</Alert>
            <Field label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} error={errors.email} />
            <Button type="submit" loading={send.isPending} className="w-full">
              Recevoir le lien
            </Button>
          </form>
        )}
        <p className="mt-6 text-center text-sm">
          <Link to="/connexion" className="font-semibold text-brand-700">
            ← Retour à la connexion
          </Link>
        </p>
      </div>
    </div>
  )
}
