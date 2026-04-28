type IconProps = { className?: string };

const base = (className?: string) => `icon${className ? ` ${className}` : ''}`;

export const FilmIcon = ({ className }: IconProps) => (
  <svg className={base(className)} viewBox="0 0 24 24" aria-hidden>
    <rect x="3" y="3" width="18" height="18" rx="2" />
    <path d="M7 3v18M17 3v18M3 8h4M3 16h4M17 8h4M17 16h4M3 12h18" />
  </svg>
);

export const SparklesIcon = ({ className }: IconProps) => (
  <svg className={base(className)} viewBox="0 0 24 24" aria-hidden>
    <path d="M12 4l1.6 4.4L18 10l-4.4 1.6L12 16l-1.6-4.4L6 10l4.4-1.6L12 4z" />
    <path d="M19 15l.7 1.8L21.5 17.5l-1.8.7L19 20l-.7-1.8L16.5 17.5l1.8-.7L19 15z" />
  </svg>
);

export const LayersIcon = ({ className }: IconProps) => (
  <svg className={base(className)} viewBox="0 0 24 24" aria-hidden>
    <path d="M12 3l9 5-9 5-9-5 9-5z" />
    <path d="M3 13l9 5 9-5" />
    <path d="M3 18l9 5 9-5" />
  </svg>
);

export const SearchIcon = ({ className }: IconProps) => (
  <svg className={base(className)} viewBox="0 0 24 24" aria-hidden>
    <circle cx="11" cy="11" r="7" />
    <path d="M20 20l-3.5-3.5" />
  </svg>
);

export const XIcon = ({ className }: IconProps) => (
  <svg className={base(className)} viewBox="0 0 24 24" aria-hidden>
    <path d="M6 6l12 12M18 6L6 18" />
  </svg>
);

export const HeartIcon = ({ className }: IconProps) => (
  <svg className={base(className)} viewBox="0 0 24 24" aria-hidden>
    <path d="M12 21s-7.5-4.5-9.5-9.5C1 8 3.5 5 6.5 5c2 0 3.5 1 5.5 3 2-2 3.5-3 5.5-3 3 0 5.5 3 4 6.5C19.5 16.5 12 21 12 21z" />
  </svg>
);

export const SkipIcon = ({ className }: IconProps) => (
  <svg className={base(className)} viewBox="0 0 24 24" aria-hidden>
    <path d="M5 5l8 7-8 7M14 5l8 7-8 7" />
  </svg>
);

export const ArrowRightIcon = ({ className }: IconProps) => (
  <svg className={base(className)} viewBox="0 0 24 24" aria-hidden>
    <path d="M5 12h14M13 5l7 7-7 7" />
  </svg>
);

export const ArrowLeftIcon = ({ className }: IconProps) => (
  <svg className={base(className)} viewBox="0 0 24 24" aria-hidden>
    <path d="M19 12H5M11 5l-7 7 7 7" />
  </svg>
);

export const StarIcon = ({ className }: IconProps) => (
  <svg className={base(className)} viewBox="0 0 24 24" aria-hidden>
    <path d="M12 3l2.7 5.7 6.3.9-4.5 4.4 1 6.3L12 17.5 6.5 20.3l1-6.3L3 9.6l6.3-.9L12 3z" />
  </svg>
);
