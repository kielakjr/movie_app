import { fetchWithCreds } from './index';

export const postSessionReset = async (): Promise<void> => {
  const response = await fetchWithCreds('/api/session/reset', { method: 'POST' });
  if (!response.ok) {
    throw new Error('Failed to reset session');
  }
};
