export default function Logo({ size = 70 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 100 100" fill="none">
      {/* outer arc */}
      <path d="M 13 67 A 40 40 0 0 1 87 67" stroke="#F5A623" strokeWidth="3.5" fill="none" />
      {/* bun top */}
      <ellipse cx="50" cy="38" rx="23" ry="13" fill="#F5A623" opacity=".92" />
      {/* seeds */}
      <circle cx="43" cy="35" r="2" fill="#d4901e" />
      <circle cx="52" cy="33" r="2" fill="#d4901e" />
      <circle cx="60" cy="36" r="2" fill="#d4901e" />
      {/* lettuce */}
      <ellipse cx="50" cy="51" rx="26" ry="4.5" fill="#22c55e" opacity=".88" />
      {/* patty */}
      <ellipse cx="50" cy="57" rx="24" ry="5.5" fill="#E91E8C" opacity=".92" />
      {/* bun bottom */}
      <ellipse cx="50" cy="65" rx="24" ry="7" fill="#F5A623" opacity=".92" />
      {/* base line */}
      <line x1="13" y1="72" x2="87" y2="72" stroke="#F5A623" strokeWidth="2" />
    </svg>
  )
}
