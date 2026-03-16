import React from 'react';

const Avatar = ({ name, size = '10' }) => {
  const firstLetter = name ? name.charAt(0).toUpperCase() : '?';
  
  // Map size string to Tailwind classes
  const sizeMap = {
    '10': 'w-10 h-10',
    '12': 'w-12 h-12',
    '16': 'w-16 h-16',
    '24': 'w-24 h-24',
  };
  
  const sizeClasses = sizeMap[size] || sizeMap['10'];
  
  // Map size to text size
  const textSizeMap = {
    '10': 'text-sm',
    '12': 'text-base',
    '16': 'text-lg',
    '24': 'text-xl',
  };
  
  const textSize = textSizeMap[size] || textSizeMap['10'];

  return (
    <div
      className={`flex items-center justify-center rounded-full bg-blue-950 text-white overflow-hidden ${sizeClasses} aspect-square`}
    >
      <span className={`${textSize} font-bold`}>{firstLetter}</span>
    </div>
  );
};

export default Avatar;
