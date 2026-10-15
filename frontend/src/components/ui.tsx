import { AlertCircle, CheckCircle2, Loader2 } from 'lucide-react'
import { useId } from 'react'
import { buttonClasses } from './buttonClasses'
import type { Variant } from './buttonClasses'
import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'

// Petits composants partages : boutons, champs, messages, chargement

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; loading?: boolean }

export function Button({ variant = 'primary', loading = false, className = '', children, disabled, ...props }: ButtonProps) {
  return (
    <button className={buttonClasses(variant, className)} disabled={loading || disabled} {...props}>
      {loading && <Loader2 className="size-4 animate-spin" aria-hidden />}
      {children}
    </button>
  )
}

const inputClasses = (error?: string) =>
  `w-full rounded-xl border bg-white px-3.5 py-2.5 text-sm outline-none transition focus:ring-4 ${
    error ? 'border-red-400 focus:ring-red-100' : 'border-slate-300 focus:border-brand-600 focus:ring-brand-100'
  }`

type FieldShellProps = { id: string; label: string; error?: string; hint?: string; children: ReactNode; className?: string }

function FieldShell({ id, label, error, hint, children, className = '' }: FieldShellProps) {
  return (
    <div className={className}>
      <label htmlFor={id} className="mb-1.5 block text-sm font-medium text-slate-700">
        {label}
      </label>
      {children}
      {hint && !error && <p className="mt-1 text-xs text-slate-500">{hint}</p>}
      {error && (
        <p id={`${id}-error`} className="mt-1.5 text-sm text-red-700">
          {error}
        </p>
      )}
    </div>
  )
}

type FieldProps = InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string; hint?: string }

export function Field({ label, error, hint, className, ...props }: FieldProps) {
  const id = useId()
  return (
    <FieldShell id={id} label={label} error={error} hint={hint} className={className}>
      <input id={id} aria-invalid={Boolean(error)} aria-describedby={error ? `${id}-error` : undefined} className={inputClasses(error)} {...props} />
    </FieldShell>
  )
}

type CheckboxProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'> & { label: ReactNode; error?: string }

/** Case a cocher avec un libelle riche (liens) et son message d'erreur. */
export function Checkbox({ label, error, className = '', ...props }: CheckboxProps) {
  const id = useId()
  return (
    <div className={className}>
      <div className="flex items-start gap-2.5">
        <input
          id={id}
          type="checkbox"
          aria-invalid={Boolean(error)}
          aria-describedby={error ? `${id}-error` : undefined}
          className="mt-0.5 size-4 shrink-0 rounded border-slate-300 accent-brand-700"
          {...props}
        />
        <label htmlFor={id} className="text-sm text-slate-700">
          {label}
        </label>
      </div>
      {error && (
        <p id={`${id}-error`} className="mt-1.5 text-sm text-red-700">
          {error}
        </p>
      )}
    </div>
  )
}

type SelectProps = SelectHTMLAttributes<HTMLSelectElement> & { label: string; error?: string; children: ReactNode }

export function Select({ label, error, className, children, ...props }: SelectProps) {
  const id = useId()
  return (
    <FieldShell id={id} label={label} error={error} className={className}>
      <select id={id} aria-invalid={Boolean(error)} className={inputClasses(error)} {...props}>
        {children}
      </select>
    </FieldShell>
  )
}

type TextAreaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & { label: string; error?: string }

export function TextArea({ label, error, className, ...props }: TextAreaProps) {
  const id = useId()
  return (
    <FieldShell id={id} label={label} error={error} className={className}>
      <textarea id={id} rows={3} aria-invalid={Boolean(error)} className={inputClasses(error)} {...props} />
    </FieldShell>
  )
}

export function Alert({ type = 'error', children }: { type?: 'error' | 'success'; children?: ReactNode }) {
  if (!children) return null
  const success = type === 'success'
  const Icon = success ? CheckCircle2 : AlertCircle
  return (
    <div
      role={success ? 'status' : 'alert'}
      className={`flex gap-2.5 rounded-xl p-3.5 text-sm ring-1 ring-inset ${
        success ? 'bg-emerald-50 text-emerald-800 ring-emerald-200' : 'bg-red-50 text-red-800 ring-red-200'
      }`}
    >
      <Icon className="mt-0.5 size-4 shrink-0" aria-hidden />
      <div>{children}</div>
    </div>
  )
}

export function Loader() {
  return (
    <div className="flex justify-center py-16">
      <Loader2 className="size-8 animate-spin text-brand-700" aria-label="Chargement" />
    </div>
  )
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl font-extrabold tracking-tight">{title}</h1>
        {subtitle && <p className="mt-1 text-sm text-slate-600">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  )
}

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <section className={`rounded-2xl bg-white p-5 ring-1 ring-slate-200 ${className}`}>{children}</section>
}

export function Badge({ tone = 'slate', children }: { tone?: 'slate' | 'red' | 'amber' | 'green' | 'brand'; children: ReactNode }) {
  const tones = {
    slate: 'bg-slate-100 text-slate-700',
    red: 'bg-red-100 text-red-800',
    amber: 'bg-amber-100 text-amber-800',
    green: 'bg-emerald-100 text-emerald-800',
    brand: 'bg-brand-50 text-brand-800',
  }
  return <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-semibold ${tones[tone]}`}>{children}</span>
}
