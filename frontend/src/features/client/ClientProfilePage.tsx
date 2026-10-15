import { Link } from 'react-router-dom'
import { ProfilePanel } from '../profile/ProfilePanel'

// Profil du client : photo, coordonnees (utilisees pour les devis et livraisons) et mot de passe
export default function ClientProfilePage() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6">
      <Link to="/mon-espace" className="text-sm font-semibold text-brand-700">
        ← Mon espace
      </Link>
      <h1 className="mt-3 text-3xl font-extrabold tracking-tight">Mon profil</h1>
      <p className="mt-1 mb-6 text-slate-600">Vos coordonnées figurent sur vos devis et nous servent à vous joindre le jour de la livraison.</p>
      <ProfilePanel />
    </div>
  )
}
