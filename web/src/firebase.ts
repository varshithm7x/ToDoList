import { initializeApp } from 'firebase/app';
import { getAuth } from 'firebase/auth';
import { getDatabase } from 'firebase/database';

const firebaseConfig = {
  apiKey: "AIzaSyA4ut2xeVYTJi0ElDW73WFMpl8quL6-A1I",
  authDomain: "todolist-f954c.firebaseapp.com",
  databaseURL: "https://todolist-f954c-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "todolist-f954c",
  storageBucket: "todolist-f954c.firebasestorage.app",
  messagingSenderId: "926593449655",
  appId: "1:926593449655:android:2cf8e108db12bbbff29732"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const database = getDatabase(app); 