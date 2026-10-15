import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, Mail, Phone } from 'lucide-react'
import { contactApi, errorMessage } from '../../api/client'
import { Alert, Badge, Button, Card, Loader, PageHeader } from '../../components/ui'
import { formatDate } from '../../lib/format'

// US-21 : messages envoyes depuis la page contact du site
export default function MessagesPage() {
  const queryClient = useQueryClient()
  const messages = useQuery({ queryKey: ['contact-messages'], queryFn: contactApi.list })
  const handled = useMutation({
    mutationFn: contactApi.handled,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['contact-messages'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })

  return (
    <>
      <PageHeader title="Messages" subtitle="Demandes reçues par le formulaire de contact du site." />
      {messages.isPending && <Loader />}
      <Alert>{messages.isError ? errorMessage(messages.error) : handled.isError ? errorMessage(handled.error) : ''}</Alert>
      {messages.data?.length === 0 && <p className="text-slate-600">Aucun message pour le moment.</p>}
      <div className="space-y-4">
        {messages.data?.map((m) => (
          <Card key={m.id} className={`space-y-2 ${m.handled ? 'opacity-70' : ''}`}>
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="font-bold">{m.name}</p>
              <Badge tone={m.handled ? 'slate' : 'amber'}>{m.handled ? 'Traité' : 'À traiter'}</Badge>
            </div>
            <p className="text-sm text-slate-500">
              Reçu le {formatDate(m.createdAt)}
              {m.eventDate ? ` · événement le ${formatDate(m.eventDate)}` : ''}
            </p>
            <p className="whitespace-pre-line text-slate-800">{m.message}</p>
            <div className="flex flex-wrap items-center gap-4 text-sm">
              <a href={`mailto:${m.email}`} className="flex items-center gap-1.5 font-semibold text-brand-700">
                <Mail className="size-4" aria-hidden />
                {m.email}
              </a>
              {m.phone && (
                <a href={`tel:${m.phone}`} className="flex items-center gap-1.5 font-semibold text-brand-700">
                  <Phone className="size-4" aria-hidden />
                  {m.phone}
                </a>
              )}
              {!m.handled && (
                <Button variant="secondary" className="ml-auto" loading={handled.isPending && handled.variables === m.id} onClick={() => handled.mutate(m.id)}>
                  <Check className="size-4" aria-hidden />
                  Marquer comme traité
                </Button>
              )}
            </div>
          </Card>
        ))}
      </div>
    </>
  )
}
