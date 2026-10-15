import { useMutation } from '@tanstack/react-query'
import { Clock, Mail, MapPin, Phone, Send } from 'lucide-react'
import { useState } from 'react'
import type { FormEvent } from 'react'
import { contactApi, errorMessage, fieldErrors } from '../../api/client'
import type { ContactForm } from '../../api/types'
import { Alert, Button, Card, Field, TextArea } from '../../components/ui'
import { COMPANY, OPENING_HOURS } from '../../lib/company'

const EMPTY: ContactForm = { name: '', email: '', phone: '', eventDate: '', message: '' }

// US-21 : page contact (coordonnees, horaires, formulaire)
export default function ContactPage() {
  const [form, setForm] = useState<ContactForm>(EMPTY)
  const send = useMutation({ mutationFn: () => contactApi.send(form), onSuccess: () => setForm(EMPTY) })
  const errors = fieldErrors(send.error)
  const set = (field: keyof ContactForm) => (event: { target: { value: string } }) => setForm({ ...form, [field]: event.target.value })

  const submit = (event: FormEvent) => {
    event.preventDefault()
    send.mutate()
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6">
      <h1 className="text-3xl font-extrabold tracking-tight sm:text-4xl">Contactez-nous</h1>
      <p className="mt-2 max-w-2xl text-slate-600">Une question, un projet sur mesure ? Écrivez-nous : nous répondons sous 24 h ouvrées.</p>
      <div className="mt-8 grid gap-6 lg:grid-cols-[1fr_22rem]">
        <Card>
          <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2" noValidate>
            <Field label="Nom" autoComplete="name" value={form.name} onChange={set('name')} error={errors.name} />
            <Field label="Email" type="email" autoComplete="email" value={form.email} onChange={set('email')} error={errors.email} />
            <Field label="Téléphone (facultatif)" type="tel" autoComplete="tel" value={form.phone} onChange={set('phone')} error={errors.phone} />
            <Field label="Date de l’événement (facultatif)" type="date" value={form.eventDate} onChange={set('eventDate')} error={errors.eventDate} />
            <TextArea label="Votre message" rows={5} className="sm:col-span-2" maxLength={2000} value={form.message} onChange={set('message')} error={errors.message} />
            <div className="space-y-3 sm:col-span-2">
              <Alert>{send.isError && !Object.keys(errors).length ? errorMessage(send.error) : ''}</Alert>
              {send.isSuccess && <Alert type="success">Merci ! Votre message est bien envoyé, nous vous répondons très vite.</Alert>}
              <Button type="submit" loading={send.isPending}>
                <Send className="size-4" aria-hidden />
                Envoyer
              </Button>
            </div>
          </form>
        </Card>
        <Card className="h-fit space-y-4 text-sm">
          <p className="flex gap-2">
            <MapPin className="mt-0.5 size-4 shrink-0 text-brand-700" aria-hidden />
            {COMPANY.address}
          </p>
          <a href={`tel:${COMPANY.phone.replace(/\s/g, '')}`} className="flex gap-2 font-semibold text-brand-700">
            <Phone className="mt-0.5 size-4 shrink-0" aria-hidden />
            {COMPANY.phone}
          </a>
          <a href={`mailto:${COMPANY.email}`} className="flex gap-2 font-semibold text-brand-700">
            <Mail className="mt-0.5 size-4 shrink-0" aria-hidden />
            {COMPANY.email}
          </a>
          <div className="flex gap-2">
            <Clock className="mt-0.5 size-4 shrink-0 text-brand-700" aria-hidden />
            <dl className="space-y-1">
              {OPENING_HOURS.map((h) => (
                <div key={h.days}>
                  <dt className="font-semibold">{h.days}</dt>
                  <dd className="text-slate-600">{h.hours}</dd>
                </div>
              ))}
            </dl>
          </div>
          <p className="text-slate-600">Zone d’intervention : {COMPANY.area}.</p>
        </Card>
      </div>
    </div>
  )
}
