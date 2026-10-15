import { PageHeader } from '../../components/ui'
import { ProfilePanel } from './ProfilePanel'

// Profil d'un employe : photo, informations et mot de passe
export default function ProfilePage() {
  return (
    <>
      <PageHeader title="Mon profil" subtitle="Votre photo apparaît dans le menu et auprès de vos collègues" />
      <ProfilePanel />
    </>
  )
}
