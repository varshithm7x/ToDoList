import { TimeSlot } from './TimeSlot';

export interface Todo {
  id: number;
  title: string;
  isCompleted: boolean;
  date: string | null;
  time: string | null;
  firebaseKey?: string;
}

export interface User {
  id: string;
  email: string;
  todos: Todo[];
} 