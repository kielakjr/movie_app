import { useMovies } from './hooks';
import { useState } from 'react';

const App = () => {
  const [page, setPage] = useState(0);
  const { data, isLoading, error } = useMovies(page, 10);
  return (
    <div>
      <h1>Movie List</h1>
      {isLoading && <p>Loading...</p>}
      {error && <p>Error: {error.message}</p>}
      <ul>
        {data?.content.map((movie) => (
          <li key={movie.id}>{movie.title}</li>
        ))}
      </ul>
      <button onClick={() => setPage((prev) => prev - 1)} disabled={page === 0}>Previous</button>
      <button onClick={() => setPage((prev) => prev + 1)} disabled={page + 1 === data?.page.total_pages}>Next</button>
    </div>
  )
}

export default App
