module.exports = {
  theme: {
    extend: {
      colors: {
        'primary': {
          DEFAULT: '#3366CC',
          'light': '#5588EE',
          'dark': '#2255BB'
        },
        'secondary': {
          DEFAULT: '#5588EE',
          'light': '#77AAFF',
          'dark': '#4477DD'
        },
        'accent': {
          'green': '#44BB77',
          'red': '#FF4444',
          'yellow': '#FFAA44',
          'blue': '#5588EE'
        },
        'neutral': {
          'dark': '#333333',
          'gray': '#888888',
          'light': '#EEEEEE'
        }
      },
      spacing: {
        '2xs': '4px',
        'xs': '8px',
        'sm': '16px',
        'md': '24px',
        'lg': '32px',
        'xl': '48px',
        '2xl': '64px'
      },
      fontSize: {
        'h1': '24px',
        'h2': '20px',
        'h3': '18px',
        'body': '16px',
        'small': '14px',
        'micro': '12px'
      },
      lineHeight: {
        'h1': '1.2',
        'h2': '1.3',
        'h3': '1.4',
        'body': '1.5',
        'small': '1.5',
        'micro': '1.4'
      },
      fontWeight: {
        'regular': '400',
        'medium': '500',
        'bold': '700'
      },
      borderRadius: {
        'DEFAULT': '4px',
        'card': '8px',
        'full': '9999px'
      },
      boxShadow: {
        'card': '0 2px 8px rgba(0,0,0,0.1)',
        'modal': '0 4px 12px rgba(0,0,0,0.15)',
      },
      maxWidth: {
        'container': '1200px'
      },
      screens: {
        'sm': '576px',
        'md': '768px',
        'lg': '992px',
        'xl': '1200px',
      }
    }
  },
  variants: {
    extend: {
      opacity: ['disabled'],
      backgroundColor: ['disabled', 'active', 'focus'],
      textColor: ['disabled'],
      cursor: ['disabled'],
    }
  },
  plugins: [
    require('@tailwindcss/forms'),
  ]
}