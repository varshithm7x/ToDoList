/// <reference types="react-scripts" />

import { User } from 'firebase/auth';

declare global {
  namespace JSX {
    interface IntrinsicElements {
      [elemName: string]: any;
    }
  }
}

declare module 'firebase/auth' {
  interface User {
    uid: string;
    email: string | null;
  }
} 