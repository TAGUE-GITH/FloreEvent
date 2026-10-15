import axios from 'axios'

// En developpement VITE_API_URL est vide : Vite transmet /api au backend (voir vite.config.ts)
export const API_URL: string = import.meta.env.VITE_API_URL ?? ''

export const api = axios.create({ baseURL: `${API_URL}/api` })

export type Health = { status: string }

export const healthApi = {
  get: () => api.get<Health>('/health').then((response) => response.data),
}
