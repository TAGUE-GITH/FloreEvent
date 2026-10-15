import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CheckCircle2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { errorMessage, maintenanceApi } from '../../api/client'
import { useAuth } from '../../auth/AuthProvider'
import { Alert, Button, Card, Loader, PageHeader } from '../../components/ui'
import { formatDate } from '../../lib/format'

// US-04 : materiel en reparation (en cours ou prevu) ; « Repare » le rend a la location des aujourd'hui
export default function MaintenancePage() {
  const { hasRole } = useAuth()
  const queryClient = useQueryClient()
  const repairs = useQuery({ queryKey: ['maintenance'], queryFn: maintenanceApi.open })
  const end = useMutation({
    mutationFn: maintenanceApi.end,
    onSuccess: () => {
      for (const key of [['maintenance'], ['items'], ['item'], ['repairs']]) queryClient.invalidateQueries({ queryKey: key })
    },
  })

  return (
    <>
      <PageHeader title="Matériel en réparation" subtitle="Exemplaires retirés de la location, en cours ou prévus" />
      {repairs.isPending && <Loader />}
      <Alert>{repairs.isError ? errorMessage(repairs.error) : end.isError ? errorMessage(end.error) : ''}</Alert>
      {repairs.data?.length === 0 && (
        <Card>
          <p className="flex items-center gap-2 text-sm text-slate-600">
            <CheckCircle2 className="size-5 text-emerald-600" aria-hidden />
            Tout le matériel est en état de marche.
          </p>
        </Card>
      )}
      {repairs.data && repairs.data.length > 0 && (
        <Card className="p-0!">
          <ul className="divide-y divide-stone-100">
            {repairs.data.map((repair) => (
              <li key={repair.id} className="flex flex-wrap items-center gap-x-4 gap-y-2 px-4 py-3">
                <div className="min-w-0 flex-1">
                  <Link to={`/gestion/catalogue/${repair.itemId}`} className="font-semibold hover:text-brand-700">
                    {repair.quantity} × {repair.itemName}
                  </Link>
                  <p className="text-sm text-slate-500">
                    Depuis le {formatDate(repair.fromDate)} {repair.toDate ? `· jusqu’au ${formatDate(repair.toDate)}` : '· jusqu’à réparation'} · {repair.reason ?? 'Sans motif'}
                  </p>
                </div>
                {hasRole('GERANT', 'LOGISTIQUE') && (
                  <Button variant="secondary" loading={end.isPending && end.variables === repair.id} onClick={() => end.mutate(repair.id)}>
                    Réparé
                  </Button>
                )}
              </li>
            ))}
          </ul>
        </Card>
      )}
    </>
  )
}
