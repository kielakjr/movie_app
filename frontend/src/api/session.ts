import { fetchWithCreds } from './index';

export type Session = {
  likesCount: number;
};

export const fetchSession = async (): Promise<Session> => {
  const response = await fetchWithCreds('/api/session');
  if (!response.ok) {
    throw new Error('Failed to load session');
  }
  return response.json();
};

export const postSessionReset = async (): Promise<void> => {
  const response = await fetchWithCreds('/api/session/reset', { method: 'POST' });
  if (!response.ok) {
    throw new Error('Failed to reset session');
  }
};
