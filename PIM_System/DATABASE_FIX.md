# Database Schema Fix

## Problem
The database table still has the `full_name` column, but we removed it from the User entity.

## Solution Options

### Option 1: Recreate Table (Recommended for Development)
1. The `application.properties` has been temporarily set to `spring.jpa.hibernate.ddl-auto=create`
2. Run your application once - it will recreate the table without the `full_name` column
3. After the first successful run, change it back to `update` in `application.properties`:
   ```
   spring.jpa.hibernate.ddl-auto=update
   ```

### Option 2: Manual SQL Fix (If you want to keep existing data)
Run this SQL in your MySQL database (via phpMyAdmin or MySQL command line):

```sql
USE pim_system;
ALTER TABLE users DROP COLUMN full_name;
```

Then change `application.properties` back to:
```
spring.jpa.hibernate.ddl-auto=update
```

## After Fix
Once fixed, your application should start successfully and the default users will be created:
- admin@example.com / admin123
- manager@example.com / manager123



