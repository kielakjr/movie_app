import { Routes } from 'react-router';
import { Route } from 'react-router';
import NavbarLayout from './layout/NavbarLayout';
import Home from './pages/Home';
import Movies from './pages/Movies';
import Swipe from './pages/Swipe';

const App = () => {
  return (
    <Routes>
      <Route element={<NavbarLayout />}>
        <Route path="/" element={<Home />} />
        <Route path="/movies" element={<Movies />} />
        <Route path="/swipe" element={<Swipe />} />
      </Route>
    </Routes>
  )
}

export default App
