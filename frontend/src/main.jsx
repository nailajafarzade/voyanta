
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from "react-router-dom";
import App from './App.jsx'
import { Provider } from "react-redux";
import './index.css'
import { PersistGate } from "redux-persist/integration/react";
import { store, persistor  } from './redux/store.jsx';
import AuthProvider from './context/AuthProvider.jsx';
import AuthModalProvider from './context/AuthModalProvider.jsx';
import PlannerProvider from './context/PlannerProvider.jsx';

ReactDOM.createRoot(document.getElementById('root')).render(
 <Provider store={store}>
    <PersistGate loading={null} persistor={persistor}>
    <BrowserRouter>
      <AuthProvider>
        <AuthModalProvider>
          <PlannerProvider>
            <App />
          </PlannerProvider>
        </AuthModalProvider>
      </AuthProvider>
    </BrowserRouter>
    </PersistGate>
  </Provider>,
)
