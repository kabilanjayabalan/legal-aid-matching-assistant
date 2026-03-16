# React Application Starter Guide

## Overview

This guide helps you set up a modern React application with essential dependencies for building a Legal Aid Matching Platform frontend.

## Prerequisites

- Node.js (v16 or higher)
- npm or yarn package manager

## Installation

### 1. Create React App

```bash
npx create-react-app legal-aid-matching-platform
cd legal-aid-matching-platform
```

### 2. Install Dependencies

```bash
npm install axios react-router-dom
npm install -D tailwindcss postcss autoprefixer
npm install zustand
npm install crypto-js
```

**Dependencies Overview:**

- **axios**: HTTP client for API requests
- **react-router-dom**: Client-side routing
- **tailwindcss**: Utility-first CSS framework
- **zustand**: Lightweight state management
- **crypto-js**: Data encryption/decryption

## Configuration

### Tailwind CSS Setup

```bash
npx tailwindcss init -p
```

Update `tailwind.config.js`:

```js
export default {
  content: ["./src/**/*.{js,jsx}"],
  theme: { extend: {} },
  plugins: [],
};
```

Update `src/index.css`:

```css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

## Project Structure

```
src/
├── components/
├── pages/
├── store/          (Zustand state)
├── utils/          (Encryption helpers)
├── services/       (Axios API calls)
├── App.jsx
└── index.css
```

## Quick Start

Run the development server:

```bash
npm start
```
