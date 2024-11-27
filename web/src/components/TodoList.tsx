import * as React from 'react';
import { useAuth } from '../contexts/AuthContext';
import { database } from '../firebase';
import { ref, onValue, update, remove, DataSnapshot, get } from 'firebase/database';
import { Todo } from '../types/Todo';
import {
  Box,
  Container,
  Typography,
  List,
  ListItem,
  ListItemText,
  Checkbox,
  IconButton,
  Paper,
  Fab,
  CircularProgress,
  Tabs,
  Tab,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
} from '@mui/material';
import { Delete as DeleteIcon, Add as AddIcon, Logout as LogoutIcon } from '@mui/icons-material';
import AddTodoDialog from './AddTodoDialog';

export default function TodoList() {
  const [todos, setTodos] = React.useState<Todo[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [isAddDialogOpen, setIsAddDialogOpen] = React.useState(false);
  const { currentUser, logout } = useAuth();
  const [selectedView, setSelectedView] = React.useState<'all' | 'simple' | 'calendar' | 'time'>('all');
  const [showLogoutConfirm, setShowLogoutConfirm] = React.useState(false);

  React.useEffect(() => {
    if (!currentUser) {
      setLoading(false);
      return;
    }

    const todosRef = ref(database, `users/${currentUser.uid}/todos`);
    const unsubscribe = onValue(todosRef, (snapshot: DataSnapshot) => {
      try {
        const data = snapshot.val();
        if (data) {
          const todoList = Object.entries(data).map(([firebaseKey, value]) => ({
            ...(value as Todo),
            firebaseKey
          }));
          setTodos(todoList.sort((a, b) => a.id - b.id));
        } else {
          setTodos([]);
        }
      } catch (error) {
        console.error('Error processing todos:', error);
        setTodos([]);
      } finally {
        setLoading(false);
      }
    });

    return () => unsubscribe();
  }, [currentUser]);

  const handleToggleComplete = async (todo: Todo) => {
    if (!currentUser || !todo.firebaseKey) return;
    try {
      const todoRef = ref(database, `users/${currentUser.uid}/todos/${todo.firebaseKey}`);
      await update(todoRef, {
        ...todo,
        isCompleted: !todo.isCompleted,
        firebaseKey: null
      });
    } catch (error) {
      console.error('Error toggling todo:', error);
    }
  };

  const handleDelete = async (todo: Todo) => {
    if (!currentUser || !todo.firebaseKey) return;
    try {
      const todoRef = ref(database, `users/${currentUser.uid}/todos/${todo.firebaseKey}`);
      await remove(todoRef);
    } catch (error) {
      console.error('Error deleting todo:', error);
    }
  };

  const filterTodos = (todos: Todo[]) => {
    switch (selectedView) {
      case 'all':
        return todos;
      case 'simple':
        return todos.filter(todo => !todo.date && !todo.time);
      case 'calendar':
        return todos.filter(todo => todo.date && !todo.time);
      case 'time':
        return todos.filter(todo => todo.time);
      default:
        return todos;
    }
  };

  const getTaskSection = (todo: Todo): 'simple' | 'calendar' | 'time' => {
    if (todo.time) return 'time';
    if (todo.date) return 'calendar';
    return 'simple';
  };

  const handleTaskClick = (todo: Todo) => {
    if (selectedView === 'all') {
      setSelectedView(getTaskSection(todo));
    }
  };

  const handleLogout = async () => {
    try {
      await logout();
      // Redirect will be handled by the AuthContext/Router
    } catch (error) {
      console.error('Failed to log out:', error);
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Container maxWidth="sm" sx={{ px: { xs: 2, sm: 3 } }}>
      <Box sx={{ 
        position: 'fixed', 
        top: { xs: 8, sm: 16 }, 
        right: { xs: 8, sm: 16 }, 
        zIndex: 1000 
      }}>
        <IconButton
          onClick={() => setShowLogoutConfirm(true)}
          sx={{ 
            backgroundColor: 'background.paper',
            '&:hover': { backgroundColor: 'action.hover' },
            boxShadow: 1,
            padding: { xs: 1, sm: 1.5 }
          }}
        >
          <LogoutIcon sx={{ fontSize: { xs: '1.2rem', sm: '1.5rem' } }} />
        </IconButton>
      </Box>

      <Dialog
        open={showLogoutConfirm}
        onClose={() => setShowLogoutConfirm(false)}
      >
        <DialogTitle>Confirm Logout</DialogTitle>
        <DialogContent>
          <Typography>Are you sure you want to log out?</Typography>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setShowLogoutConfirm(false)}>Cancel</Button>
          <Button onClick={handleLogout} color="primary">
            Logout
          </Button>
        </DialogActions>
      </Dialog>

      <Box sx={{ 
        mt: { xs: 6, sm: 4 }, 
        mb: 4,
        width: '100%'
      }}>
        <Paper sx={{ mb: 2 }}>
          <Box sx={{ borderBottom: 1, borderColor: 'divider' }}>
            <Tabs
              value={selectedView}
              onChange={(event: React.SyntheticEvent, newValue: 'all' | 'simple' | 'calendar' | 'time') => 
                setSelectedView(newValue)
              }
              variant="scrollable"
              scrollButtons="auto"
              allowScrollButtonsMobile
              sx={{
                '& .MuiTab-root': {
                  minWidth: { xs: 'auto', sm: 90 },
                  px: { xs: 2, sm: 3 },
                  fontSize: { xs: '0.8rem', sm: '0.875rem' }
                }
              }}
            >
              <Tab label="All Tasks" value="all" />
              <Tab label="Simple" value="simple" />
              <Tab label="Calendar" value="calendar" />
              <Tab label="Time Slots" value="time" />
            </Tabs>
          </Box>
        </Paper>

        <Typography 
          variant="h6" 
          sx={{ 
            mb: 2,
            fontSize: { xs: '1.1rem', sm: '1.25rem' }
          }}
        >
          {selectedView === 'all' && 'All Tasks'}
          {selectedView === 'simple' && 'Simple Tasks'}
          {selectedView === 'calendar' && 'Calendar Tasks'}
          {selectedView === 'time' && 'Time-Slotted Tasks'}
        </Typography>

        {filterTodos(todos).length === 0 ? (
          <Paper 
            elevation={3} 
            sx={{ 
              p: { xs: 2, sm: 4 }, 
              display: 'flex', 
              flexDirection: 'column', 
              alignItems: 'center',
              backgroundColor: 'background.default'
            }}
          >
            <Typography 
              variant="h6" 
              sx={{ 
                mb: 2, 
                textAlign: 'center',
                fontSize: { xs: '1rem', sm: '1.25rem' }
              }}
            >
              No tasks in this section
            </Typography>
            <Typography 
              color="textSecondary" 
              textAlign="center"
              sx={{ fontSize: { xs: '0.875rem', sm: '1rem' } }}
            >
              Click the + button to add a new task
            </Typography>
          </Paper>
        ) : (
          <List sx={{ p: 0 }}>
            {filterTodos(todos).map((todo: Todo) => (
              <ListItem
                key={todo.id}
                onClick={() => handleTaskClick(todo)}
                sx={{
                  mb: { xs: 1, sm: 1.5 },
                  px: { xs: 1, sm: 2 },
                  py: { xs: 0.5, sm: 1 },
                  backgroundColor: 'background.paper',
                  borderRadius: 1,
                  cursor: selectedView === 'all' ? 'pointer' : 'default',
                  '&:hover': selectedView === 'all' ? {
                    backgroundColor: 'action.hover',
                  } : {},
                }}
              >
                <Box sx={{ 
                  display: 'flex', 
                  alignItems: 'center', 
                  width: '100%',
                  gap: { xs: 1, sm: 2 }
                }}>
                  <Checkbox
                    checked={todo.isCompleted}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => {
                      e.stopPropagation();
                      handleToggleComplete(todo);
                    }}
                    onClick={(e: React.MouseEvent) => e.stopPropagation()}
                    color="primary"
                    sx={{ 
                      p: { xs: 0.5, sm: 1 },
                      '& .MuiSvgIcon-root': {
                        fontSize: { xs: '1.2rem', sm: '1.5rem' }
                      }
                    }}
                  />
                  <ListItemText
                    primary={todo.title}
                    secondary={
                      <>
                        {selectedView === 'all' && (
                          <Typography 
                            component="span" 
                            color="text.secondary" 
                            sx={{ 
                              mr: 1,
                              fontSize: { xs: '0.75rem', sm: '0.875rem' }
                            }}
                          >
                            [{getTaskSection(todo).charAt(0).toUpperCase() + getTaskSection(todo).slice(1)}]
                          </Typography>
                        )}
                        <Typography 
                          component="span" 
                          sx={{ fontSize: { xs: '0.75rem', sm: '0.875rem' } }}
                        >
                          {todo.date && todo.time
                            ? `${todo.date} at ${todo.time}`
                            : todo.date
                            ? `Due: ${todo.date}`
                            : todo.time
                            ? `Time: ${todo.time}`
                            : null}
                        </Typography>
                      </>
                    }
                    sx={{
                      textDecoration: todo.isCompleted ? 'line-through' : 'none',
                      '& .MuiTypography-root': {
                        fontSize: { xs: '0.9rem', sm: '1rem' }
                      }
                    }}
                  />
                  <IconButton 
                    edge="end" 
                    onClick={(e: React.MouseEvent) => {
                      e.stopPropagation();
                      handleDelete(todo);
                    }}
                    sx={{ 
                      p: { xs: 0.5, sm: 1 },
                      '& .MuiSvgIcon-root': {
                        fontSize: { xs: '1.2rem', sm: '1.5rem' }
                      }
                    }}
                  >
                    <DeleteIcon />
                  </IconButton>
                </Box>
              </ListItem>
            ))}
          </List>
        )}
      </Box>
      
      {selectedView !== 'all' && (
        <Fab
          color="primary"
          sx={{ 
            position: 'fixed', 
            bottom: { xs: 16, sm: 24 }, 
            right: { xs: 16, sm: 24 },
            width: { xs: 48, sm: 56 },
            height: { xs: 48, sm: 56 }
          }}
          onClick={() => setIsAddDialogOpen(true)}
        >
          <AddIcon sx={{ fontSize: { xs: '1.5rem', sm: '2rem' } }} />
        </Fab>
      )}

      <AddTodoDialog
        open={isAddDialogOpen}
        onClose={() => setIsAddDialogOpen(false)}
        selectedView={selectedView === 'all' ? 'simple' : selectedView}
      />
    </Container>
  );
} 