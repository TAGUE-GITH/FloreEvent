import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { UserPlus } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { errorMessage, fieldErrors, usersApi } from '../../api/client'
import { ROLE_LABELS, STAFF_ROLES } from '../../api/types'
import type { NewUser, Role, User } from '../../api/types'
import { useAuth } from '../../auth/AuthProvider'
import { Alert, Badge, Button, Card, Field, Loader, PageHeader, Select } from '../../components/ui'

// Les comptes clients se creent depuis le site : la page Equipe ne propose que les roles de l'equipe
const ROLES: Role[] = STAFF_ROLES
const EMPTY: NewUser = { email: '', firstName: '', lastName: '', phone: '', role: 'COMMERCIAL', password: '' }

// US-02 : le gerant cree les comptes, change les roles et desactive les comptes
export default function UsersPage() {
  const { user: me } = useAuth()
  const queryClient = useQueryClient()
  const { data, isPending } = useQuery({ queryKey: ['users'], queryFn: usersApi.list })
  const [form, setForm] = useState<NewUser>(EMPTY)
  const [message, setMessage] = useState({ type: 'error' as 'error' | 'success', text: '' })
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['users'] })

  const create = useMutation({
    mutationFn: () => usersApi.create(form),
    onSuccess: (created) => {
      setMessage({
        type: 'success',
        text: `Compte créé pour ${created.fullName}. Donnez-lui son email et son mot de passe ; il pourra le changer plus tard.`,
      })
      setForm(EMPTY)
      refresh()
    },
  })
  const errors = fieldErrors(create.error)
  const set = (changes: Partial<NewUser>) => setForm((current) => ({ ...current, ...changes }))

  const update = async (user: User, changes: Partial<Pick<User, 'role' | 'active'>>) => {
    setMessage({ type: 'error', text: '' })
    try {
      await usersApi.update(user.id, changes)
      refresh()
    } catch (err) {
      setMessage({ type: 'error', text: errorMessage(err) })
    }
  }

  const submit = (event: FormEvent) => {
    event.preventDefault()
    setMessage({ type: 'error', text: '' })
    create.mutate()
  }

  return (
    <>
      <PageHeader title="Employés" subtitle="Chaque employé a son compte et ne voit que ce qui concerne son rôle" />
      <div className="grid gap-6 lg:grid-cols-[1fr_22rem]">
        <section>
          <div className="mb-3">
            <Alert type={message.type}>{message.text}</Alert>
          </div>
          {isPending && <Loader />}
          <ul className="divide-y divide-slate-100 rounded-2xl bg-white ring-1 ring-slate-200">
            {data?.map((user) => {
              const self = user.id === me?.id
              return (
                <li key={user.id} className={`flex flex-wrap items-center gap-3 p-4 ${user.active ? '' : 'opacity-60'}`}>
                  <div className="min-w-0 flex-1">
                    <p className="font-semibold">
                      {user.fullName} {self && <span className="text-xs font-normal text-slate-500">(vous)</span>}{' '}
                      {!user.active && <Badge tone="red">Désactivé</Badge>}
                    </p>
                    <p className="text-sm text-slate-500">
                      {user.email}
                      {user.phone && ` · ${user.phone}`}
                    </p>
                  </div>
                  <select
                    value={user.role}
                    disabled={self}
                    onChange={(event) => update(user, { role: event.target.value as Role })}
                    aria-label={`Rôle de ${user.fullName}`}
                    className="rounded-lg border border-slate-300 bg-white px-2.5 py-1.5 text-sm disabled:bg-slate-50"
                  >
                    {ROLES.map((role) => (
                      <option key={role} value={role}>
                        {ROLE_LABELS[role]}
                      </option>
                    ))}
                  </select>
                  {!self && (
                    <Button variant="ghost" className="px-3 py-1.5" onClick={() => update(user, { active: !user.active })}>
                      {user.active ? 'Désactiver' : 'Réactiver'}
                    </Button>
                  )}
                </li>
              )
            })}
          </ul>
        </section>

        <Card className="h-fit">
          <h2 className="mb-3 flex items-center gap-2 font-bold">
            <UserPlus className="size-5 text-brand-700" aria-hidden />
            Nouvel employé
          </h2>
          <form onSubmit={submit} className="space-y-3" noValidate>
            <Alert>{create.isError && !Object.keys(errors).length ? errorMessage(create.error) : ''}</Alert>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Prénom" value={form.firstName} onChange={(e) => set({ firstName: e.target.value })} error={errors.firstName} />
              <Field label="Nom" value={form.lastName} onChange={(e) => set({ lastName: e.target.value })} error={errors.lastName} />
            </div>
            <Field label="Email" type="email" value={form.email} onChange={(e) => set({ email: e.target.value })} error={errors.email} />
            <Field label="Téléphone" type="tel" value={form.phone} onChange={(e) => set({ phone: e.target.value })} />
            <Select label="Rôle" value={form.role} onChange={(e) => set({ role: e.target.value as Role })}>
              {ROLES.map((role) => (
                <option key={role} value={role}>
                  {ROLE_LABELS[role]}
                </option>
              ))}
            </Select>
            <Field label="Mot de passe provisoire" type="text" value={form.password} onChange={(e) => set({ password: e.target.value })} error={errors.password} hint="8 caractères minimum" autoComplete="new-password" />
            <Button type="submit" loading={create.isPending} className="w-full">
              Créer le compte
            </Button>
          </form>
        </Card>
      </div>
    </>
  )
}
