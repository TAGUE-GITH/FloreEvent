import { useMutation } from '@tanstack/react-query'
import { Camera, KeyRound, Trash2 } from 'lucide-react'
import { useRef, useState } from 'react'
import type { ChangeEvent, FormEvent } from 'react'
import { errorMessage, fieldErrors, profileApi } from '../../api/client'
import { ROLE_LABELS } from '../../api/types'
import type { User } from '../../api/types'
import { useAuth } from '../../auth/AuthProvider'
import { Avatar } from '../../components/Avatar'
import { Alert, Button, Card, Field } from '../../components/ui'
import { resizeImage } from '../../lib/image'

/** Espace personnel commun aux employes et aux clients : photo, informations et mot de passe. */
export function ProfilePanel() {
  const { user } = useAuth()
  if (!user) return null
  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <div className="space-y-6">
        <PhotoCard user={user} />
        <InfoCard user={user} />
      </div>
      <PasswordCard />
    </div>
  )
}

function PhotoCard({ user }: { user: User }) {
  const { setUser } = useAuth()
  const input = useRef<HTMLInputElement>(null)
  const [error, setError] = useState('')
  const upload = useMutation({ mutationFn: profileApi.setAvatar, onSuccess: setUser, onError: (err) => setError(errorMessage(err)) })
  const remove = useMutation({ mutationFn: profileApi.removeAvatar, onSuccess: setUser })

  const choose = async (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setError('')
    try {
      upload.mutate(await resizeImage(file, 512))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Image illisible.')
    }
  }

  return (
    <Card className="flex flex-wrap items-center gap-5">
      <Avatar user={user} size="lg" />
      <div className="min-w-0 flex-1">
        <p className="text-lg font-bold">{user.fullName}</p>
        <p className="text-sm text-slate-500">
          {ROLE_LABELS[user.role]} · {user.email}
        </p>
        <div className="mt-3 flex flex-wrap gap-2">
          <input ref={input} type="file" accept="image/jpeg,image/png" className="sr-only" onChange={choose} aria-label="Choisir une photo de profil" />
          <Button variant="secondary" loading={upload.isPending} onClick={() => input.current?.click()}>
            <Camera className="size-4" aria-hidden />
            {user.avatarUrl ? 'Changer la photo' : 'Ajouter une photo'}
          </Button>
          {user.avatarUrl && (
            <Button variant="ghost" loading={remove.isPending} onClick={() => remove.mutate()}>
              <Trash2 className="size-4" aria-hidden />
              Retirer
            </Button>
          )}
        </div>
        <div className="mt-2">
          <Alert>{error}</Alert>
        </div>
      </div>
    </Card>
  )
}

function InfoCard({ user }: { user: User }) {
  const { setUser } = useAuth()
  const [form, setForm] = useState({ firstName: user.firstName, lastName: user.lastName, phone: user.phone ?? '' })
  const [saved, setSaved] = useState(false)
  const save = useMutation({
    mutationFn: () => profileApi.update(form),
    onSuccess: (updated) => {
      setUser(updated)
      setSaved(true)
    },
  })
  const errors = fieldErrors(save.error)
  const submit = (event: FormEvent) => {
    event.preventDefault()
    setSaved(false)
    save.mutate()
  }

  return (
    <Card>
      <h2 className="mb-3 font-bold">Mes informations</h2>
      <form onSubmit={submit} className="space-y-3" noValidate>
        <Alert type="success">{saved ? 'Informations enregistrées.' : ''}</Alert>
        <Alert>{save.isError && !Object.keys(errors).length ? errorMessage(save.error) : ''}</Alert>
        <div className="grid grid-cols-2 gap-3">
          <Field label="Prénom" value={form.firstName} onChange={(e) => setForm({ ...form, firstName: e.target.value })} error={errors.firstName} />
          <Field label="Nom" value={form.lastName} onChange={(e) => setForm({ ...form, lastName: e.target.value })} error={errors.lastName} />
        </div>
        <Field label="Téléphone" type="tel" value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} />
        <Button type="submit" variant="secondary" loading={save.isPending}>
          Enregistrer
        </Button>
      </form>
    </Card>
  )
}

function PasswordCard() {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [done, setDone] = useState(false)
  const change = useMutation({
    mutationFn: () => profileApi.changePassword(current, next),
    onSuccess: () => {
      setDone(true)
      setCurrent('')
      setNext('')
    },
  })
  const errors = fieldErrors(change.error)
  const submit = (event: FormEvent) => {
    event.preventDefault()
    setDone(false)
    change.mutate()
  }

  return (
    <Card className="h-fit">
      <h2 className="mb-3 flex items-center gap-2 font-bold">
        <KeyRound className="size-5 text-brand-700" aria-hidden />
        Mot de passe
      </h2>
      <form onSubmit={submit} className="space-y-3" noValidate>
        <Alert type="success">{done ? 'Mot de passe modifié.' : ''}</Alert>
        <Alert>{change.isError && !Object.keys(errors).length ? errorMessage(change.error) : ''}</Alert>
        <Field label="Mot de passe actuel" type="password" autoComplete="current-password" value={current} onChange={(e) => setCurrent(e.target.value)} error={errors.currentPassword} />
        <Field label="Nouveau mot de passe" type="password" autoComplete="new-password" value={next} onChange={(e) => setNext(e.target.value)} error={errors.newPassword} hint="8 caractères minimum" />
        <Button type="submit" variant="secondary" loading={change.isPending}>
          Changer le mot de passe
        </Button>
      </form>
    </Card>
  )
}
