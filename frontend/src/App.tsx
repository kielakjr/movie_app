import { Routes } from 'react-router';
import { Route } from 'react-router';
import NavbarLayout from './layout/NavbarLayout';
import Home from './pages/Home';

const App = () => {
  return (
    <Routes>
      <Route element={<NavbarLayout />}>
        <Route path="/" element={<Home />} />
      </Route>
    </Routes>
  )
}

export default App
