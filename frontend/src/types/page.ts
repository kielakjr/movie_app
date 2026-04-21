export interface PaginationData<T> {
  content: T[];
  page: {
    size: number;
    total_elements: number;
    total_pages: number;
    number: number;
  }
}
