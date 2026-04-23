# Deployment Guide - Online Voting System

## Quick Deploy Options

### Option 1: Render (Recommended - Free Tier)

#### Deploy Backend:
1. Go to https://render.com and sign up/login
2. Click "New +" → "Web Service"
3. Connect your GitHub repository: `VanshikaChaudhary12/online-voting-system`
4. Configure:
   - **Name**: voting-system-backend
   - **Root Directory**: backend
   - **Environment**: Java
   - **Build Command**: `mvn clean package -DskipTests`
   - **Start Command**: `java -jar target/online-voting-system-0.0.1-SNAPSHOT.jar`
   - **Instance Type**: Free
5. Add Environment Variables:
   - `SERVER_PORT` = `8082`
6. Click "Create Web Service"
7. Copy the backend URL (e.g., https://voting-system-backend.onrender.com)

#### Deploy Frontend:
1. In Render, click "New +" → "Static Site"
2. Connect the same GitHub repository
3. Configure:
   - **Name**: voting-system-frontend
   - **Root Directory**: frontend
   - **Build Command**: `npm install && npm run build`
   - **Publish Directory**: dist
4. Add Environment Variable:
   - `VITE_API_BASE_URL` = `https://voting-system-backend.onrender.com` (your backend URL)
5. Click "Create Static Site"

---

### Option 2: Railway (Alternative - Free Trial)

#### Deploy Backend:
1. Go to https://railway.app and sign up
2. Click "New Project" → "Deploy from GitHub repo"
3. Select your repository
4. Click "Add variables":
   - `PORT` = `8082`
5. Railway will auto-detect Spring Boot and deploy
6. Copy the backend URL

#### Deploy Frontend:
1. In Railway, click "New" → "GitHub Repo"
2. Select the same repository
3. Add variables:
   - `VITE_API_BASE_URL` = (your backend URL)
4. Deploy

---

### Option 3: Vercel (Frontend) + Render (Backend)

#### Backend on Render (same as Option 1)

#### Frontend on Vercel:
1. Go to https://vercel.com and sign up
2. Click "Add New" → "Project"
3. Import your GitHub repository
4. Configure:
   - **Framework Preset**: Vite
   - **Root Directory**: frontend
   - **Build Command**: `npm run build`
   - **Output Directory**: dist
5. Add Environment Variable:
   - `VITE_API_BASE_URL` = (your Render backend URL)
6. Click "Deploy"

---

## Important: Update CORS Configuration

After deploying, update the backend CORS configuration:

Edit `backend/src/main/java/com/ovs/backend/config/CorsConfig.java`:

```java
configuration.setAllowedOrigins(List.of(
    "http://localhost:5173",
    "https://your-frontend-url.vercel.app",  // Add your frontend URL
    "https://your-frontend-url.onrender.com"  // Or Render URL
));
```

Then commit and push:
```bash
git add .
git commit -m "Update CORS for production"
git push origin main
```

---

## Demo Accounts (After Deployment)

- **Platform Admin**: admin@ovs.local / Password123!
- **Org Admin**: orgadmin@ovs.local / Password123!
- **Voter**: voter@ovs.local / Password123!

---

## Troubleshooting

### Backend Issues:
- Check logs in Render/Railway dashboard
- Ensure Java 21 is selected
- Verify environment variables are set

### Frontend Issues:
- Ensure `VITE_API_BASE_URL` points to backend URL
- Check browser console for CORS errors
- Verify backend is running before testing frontend

### Database:
- H2 in-memory database resets on each deployment
- For production, consider PostgreSQL (free on Render)

---

## Cost:
- **Render Free Tier**: Backend + Frontend = $0/month
- **Vercel Free Tier**: Frontend = $0/month
- **Railway Free Trial**: $5 credit (then paid)

---

## Next Steps After Deployment:

1. Test the live application
2. Share the frontend URL
3. Monitor logs for any errors
4. Consider upgrading to PostgreSQL for persistent data
