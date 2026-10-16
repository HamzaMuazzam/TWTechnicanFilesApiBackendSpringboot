import { Link as RouterLink } from 'react-router';
import { Box, Button, Typography } from '@mui/material';
import SearchOffIcon from '@mui/icons-material/SearchOff';

export function NotFoundPage() {
  return (
    <Box sx={{ flex: 1, display: 'grid', placeItems: 'center', py: 10, textAlign: 'center' }}>
      <Box>
        <SearchOffIcon sx={{ fontSize: 56, color: 'text.disabled' }} />
        <Typography variant="h4" sx={{ mt: 1 }}>
          Page not found
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1, mb: 3 }}>
          The address you opened does not exist in the admin console.
        </Typography>
        <Button component={RouterLink} to="/" variant="contained">
          Go to Overview
        </Button>
      </Box>
    </Box>
  );
}
