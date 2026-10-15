import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, Pencil, Trash2 } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { categoriesApi, errorMessage, fieldErrors } from '../../api/client'
import type { Category } from '../../api/types'
import { Alert, Button, Card, Field, Loader, PageHeader } from '../../components/ui'

// Categories du catalogue (gerant) : ajout, renommage, suppression d'une categorie vide
export default function CategoriesPage() {
  const queryClient = useQueryClient()
  const categories = useQuery({ queryKey: ['categories'], queryFn: categoriesApi.list })
  const [name, setName] = useState('')
  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['categories'] })
    queryClient.invalidateQueries({ queryKey: ['items'] })
  }
  const create = useMutation({
    mutationFn: () => categoriesApi.create(name),
    onSuccess: () => {
      setName('')
      refresh()
    },
  })
  const remove = useMutation({ mutationFn: categoriesApi.remove, onSuccess: refresh })
  const errors = fieldErrors(create.error)

  const submit = (event: FormEvent) => {
    event.preventDefault()
    create.mutate()
  }

  return (
    <>
      <PageHeader title="Catégories" subtitle="Familles d’articles du catalogue" />
      <div className="grid gap-6 lg:grid-cols-[1fr_22rem]">
        <div className="min-w-0">
          {categories.isPending && <Loader />}
          <Alert>{remove.isError ? errorMessage(remove.error) : ''}</Alert>
          {categories.data?.length === 0 && <p className="text-sm text-slate-500">Aucune catégorie pour le moment.</p>}
          {categories.data && categories.data.length > 0 && (
            <Card className="p-0!">
              <ul className="divide-y divide-stone-100">
                {categories.data.map((category) => (
                  <CategoryRow key={category.id} category={category} onChanged={refresh} onDelete={() => remove.mutate(category.id)} />
                ))}
              </ul>
            </Card>
          )}
        </div>
        <Card className="h-fit">
          <h2 className="mb-3 font-bold">Nouvelle catégorie</h2>
          <form onSubmit={submit} className="space-y-3" noValidate>
            <Alert>{create.isError && !Object.keys(errors).length ? errorMessage(create.error) : ''}</Alert>
            <Field label="Nom" value={name} onChange={(e) => setName(e.target.value)} error={errors.name} placeholder="Tentes et barnums" />
            <Button type="submit" loading={create.isPending} className="w-full">
              Ajouter
            </Button>
          </form>
        </Card>
      </div>
    </>
  )
}

function CategoryRow({ category, onChanged, onDelete }: { category: Category; onChanged: () => void; onDelete: () => void }) {
  const [editing, setEditing] = useState(false)
  const [name, setName] = useState(category.name)
  const rename = useMutation({
    mutationFn: () => categoriesApi.rename(category.id, name),
    onSuccess: () => {
      setEditing(false)
      onChanged()
    },
  })

  return (
    <li className="flex flex-wrap items-center gap-3 px-4 py-3">
      {editing ? (
        <form
          className="flex flex-1 items-end gap-2"
          onSubmit={(event) => {
            event.preventDefault()
            rename.mutate()
          }}
        >
          <Field label="Nouveau nom" value={name} onChange={(e) => setName(e.target.value)} error={rename.isError ? errorMessage(rename.error) : undefined} className="flex-1" />
          <Button type="submit" variant="secondary" loading={rename.isPending} aria-label="Enregistrer le nom">
            <Check className="size-4" />
          </Button>
        </form>
      ) : (
        <>
          <div className="min-w-0 flex-1">
            <p className="font-semibold">{category.name}</p>
            <p className="text-sm text-slate-500">{category.itemCount} article(s)</p>
          </div>
          <button type="button" onClick={() => setEditing(true)} className="rounded-lg p-2 text-slate-500 hover:bg-stone-100" aria-label={`Renommer ${category.name}`}>
            <Pencil className="size-4" />
          </button>
          <button type="button" onClick={onDelete} className="rounded-lg p-2 text-slate-500 hover:bg-red-50 hover:text-red-700" aria-label={`Supprimer ${category.name}`}>
            <Trash2 className="size-4" />
          </button>
        </>
      )}
    </li>
  )
}
