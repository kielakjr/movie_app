export * from './movies';
export * from './swipe';
export * from './recommend';
export * from './session';

export function fetchWithCreds(
  input: RequestInfo | URL,
  init: RequestInit = {}
): Promise<Response> {
  return fetch(input, {
    ...init,
    credentials: init.credentials ?? "include",
  });
}
