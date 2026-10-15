// Classes des boutons, partagees par les boutons et les liens qui ressemblent a des boutons
type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'

export type { Variant }

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-brand-700 text-white hover:bg-brand-800',
  secondary: 'bg-white text-ink ring-1 ring-slate-300 hover:bg-slate-50',
  ghost: 'text-slate-700 hover:bg-slate-100',
  danger: 'bg-white text-red-700 ring-1 ring-red-200 hover:bg-red-50',
}

export function buttonClasses(variant: Variant = 'primary', extra = '') {
  return `inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-60 ${VARIANTS[variant]} ${extra}`
}
