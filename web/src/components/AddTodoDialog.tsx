import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { database } from '../firebase';
import { ref, push, set } from 'firebase/database';
import { Todo } from '../types/Todo';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Box
} from '@mui/material';
import { 
  DatePicker,
  TimePicker
} from '@mui/x-date-pickers';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDateFns } from '@mui/x-date-pickers/AdapterDateFns';
import { format } from 'date-fns';

interface AddTodoDialogProps {
  open: boolean;
  onClose: () => void;
  selectedView: 'simple' | 'calendar' | 'time';
}

export default function AddTodoDialog({ open, onClose, selectedView }: AddTodoDialogProps) {
  const [title, setTitle] = useState('');
  const [date, setDate] = useState<Date | null>(null);
  const [time, setTime] = useState<Date | null>(null);
  const { currentUser } = useAuth();

  const handleSubmit = async () => {
    if (!currentUser || !title.trim()) return;
    
    try {
      const todosRef = ref(database, `users/${currentUser.uid}/todos`);
      const newTodoRef = push(todosRef);
      const todoId = parseInt(newTodoRef.key!, 36);

      const todoData: Todo = {
        id: todoId,
        title: title.trim(),
        isCompleted: false,
        date: date ? format(date, 'yyyy-MM-dd') : null,
        time: time ? format(time, 'HH:mm') : null
      };

      await set(newTodoRef, todoData);

      setTitle('');
      setDate(null);
      setTime(null);
      onClose();
    } catch (error) {
      console.error('Error adding todo:', error);
    }
  };

  const handleTitleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setTitle(e.target.value);
  };

  const handleDateChange = (newDate: Date | null) => {
    setDate(newDate);
  };

  const handleTimeChange = (newTime: Date | null) => {
    setTime(newTime);
  };

  return (
    <Dialog 
      open={open} 
      onClose={onClose} 
      maxWidth="sm" 
      fullWidth
      PaperProps={{
        sx: {
          m: { xs: 2, sm: 3 },
          width: { xs: 'calc(100% - 32px)', sm: 'calc(100% - 48px)' }
        }
      }}
    >
      <DialogTitle sx={{ 
        px: { xs: 2, sm: 3 },
        py: { xs: 1.5, sm: 2 },
        fontSize: { xs: '1.1rem', sm: '1.25rem' }
      }}>
        {selectedView === 'simple' && 'Add Simple Task'}
        {selectedView === 'calendar' && 'Add Calendar Task'}
        {selectedView === 'time' && 'Add Time-Based Task'}
      </DialogTitle>
      <DialogContent sx={{ px: { xs: 2, sm: 3 } }}>
        <Box sx={{ 
          display: 'flex', 
          flexDirection: 'column', 
          gap: { xs: 1.5, sm: 2 }, 
          mt: { xs: 1, sm: 2 }
        }}>
          <TextField
            autoFocus
            label="Task Title"
            fullWidth
            value={title}
            onChange={handleTitleChange}
            sx={{ '& .MuiInputBase-input': { fontSize: { xs: '0.9rem', sm: '1rem' } } }}
          />
          
          {(selectedView === 'calendar' || selectedView === 'time') && (
            <LocalizationProvider dateAdapter={AdapterDateFns}>
              <DatePicker
                label="Due Date"
                value={date}
                onChange={handleDateChange}
                slotProps={{
                  textField: {
                    sx: { '& .MuiInputBase-input': { fontSize: { xs: '0.9rem', sm: '1rem' } } }
                  }
                }}
              />
            </LocalizationProvider>
          )}

          {selectedView === 'time' && (
            <LocalizationProvider dateAdapter={AdapterDateFns}>
              <TimePicker
                label="Time"
                value={time}
                onChange={handleTimeChange}
                ampm
                minutesStep={5}
                slotProps={{
                  textField: {
                    sx: { '& .MuiInputBase-input': { fontSize: { xs: '0.9rem', sm: '1rem' } } }
                  }
                }}
              />
            </LocalizationProvider>
          )}
        </Box>
      </DialogContent>
      <DialogActions sx={{ 
        px: { xs: 2, sm: 3 }, 
        py: { xs: 1.5, sm: 2 }
      }}>
        <Button 
          onClick={onClose}
          sx={{ fontSize: { xs: '0.8rem', sm: '0.875rem' } }}
        >
          Cancel
        </Button>
        <Button 
          onClick={handleSubmit} 
          variant="contained" 
          disabled={!title.trim()}
          sx={{ fontSize: { xs: '0.8rem', sm: '0.875rem' } }}
        >
          Add Task
        </Button>
      </DialogActions>
    </Dialog>
  );
} 