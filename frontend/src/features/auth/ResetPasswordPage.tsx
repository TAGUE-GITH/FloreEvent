import { useMutation } from '@tanstack/react-query'
import { CheckCircle2, KeyRound } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { authApi, errorMessage, fieldErrors } from '../../api/client'
import { Alert, Button, Field } from '../../components/ui'
import { buttonClasses } from '../../components/buttonClasses'

// Nouveau mot de passe, choisi depuis le lien recu par email
export default function ResetPasswordPage() {
  const token = useSearchParams()[0].get('token') ?? ''
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [mismatch, setMismatch] = useState(false)
  const reset = useMutation({ mutationFn: () => authApi.resetPassword(token, password) })
  const errors = fieldErrors(reset.error)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    setMismatch(password !== confirm)
    if (password === confirm) reset.mutate()
  }

  return (
    <div className="flex justify-center px-4 py-14">
      <div className="w-full max-w-sm">
        <div className="mb-8 flex flex-col items-center text-center">
          <span className="flex size-12 items-center justify-center rounded-2xl bg-brand-700 text-white">
            <KeyRound className="size-6" aria-hidden />
          </span>
          <h1 className="mt-4 text-2xl font-extrabold tracking-tight">Nouveau mot de passe</h1>
        </div>
        {reset.isSuccess ? (
          <div className="rounded-2xl bg-white p-6 text-center ring-1 ring-stone-200">
            <CheckCircle2 className="mx-auto size-10 text-emerald-600" aria-hidden />
            <p className="mt-3 font-semibold">Mot de passe modifié</p>
            <p className="mt-1 text-sm text-slate-600">Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.</p>
            <Link to="/connexion" className={buttonClasses('primary', 'mt-5 w-full')}>
              Se connecter
            </Link>
          </div>
        ) : !token ? (
          <Alert>Ce lien est incomplet. Ouvrez le lien reçu par email, ou demandez-en un nouveau.</Alert>
        ) : (
          <form onSubmit={submit} className="space-y-4 rounded-2xl bg-white p-6 ring-1 ring-stone-200" noValidate>
            <Alert>{reset.isError && !Object.keys(errors).length ? errorMessage(reset.error) : ''}</Alert>
            <Field label="Nouveau mot de passe" type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} error={errors.password} hint="8 caractères minimum" />
            <Field
              label="Confirmer le mot de passe"
              type="password"
              autoComplete="new-password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              error={mismatch ? 'Les deux mots de passe ne sont pas identiques.' : undefined}
            />
            <Button type="submit" loading={reset.isPending} className="w-full">
              Enregistrer
            </Button>
          </form>
        )}
        <p className="mt-6 text-center text-sm">
          <Link to="/mot-de-passe-oublie" className="font-semibold text-brand-700">
            Demander un nouveau lien
          </Link>
        </p>
      </div>
    </div>
  )
}
