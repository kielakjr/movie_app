import { fetchWithCreds } from './index';

export type Capabilities = {
  semantic_search: boolean;
};

const fetchCapabilities = async (): Promise<Capabilities> => {
  const response = await fetchWithCreds('/api/capabilities');
  if (!response.ok) {
    throw new Error('Failed to fetch capabilities');
  }
  return response.json();
};

export { fetchCapabilities };
