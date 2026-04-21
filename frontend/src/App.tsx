import { Routes } from 'react-router';
import { Route } from 'react-router';
import NavbarLayout from './layout/NavbarLayout';
import Home from './pages/Home';
import Movies from './pages/Movies';

const App = () => {
  return (
    <Routes>
      <Route element={<NavbarLayout />}>
        <Route path="/" element={<Home />} />
        <Route path="/movies" element={<Movies />} />
      </Route>
    </Routes>
  )
}

export default App
