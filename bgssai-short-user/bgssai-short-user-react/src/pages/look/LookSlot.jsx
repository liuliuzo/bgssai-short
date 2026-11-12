export default function LookSlot({ label, note, action, onAction, disabled, children }) {
  const filled = !!children
  return (
    <div style={{ width: 104, flexShrink: 0 }}>
      <div style={{ fontSize: 12, marginBottom: 6, color: '#595959' }}>
        {label}
        {note ? <span style={{ color: '#d48806' }}> · {note}</span> : null}
      </div>
      {children}
      <button
        type="button"
        disabled={disabled}
        onClick={onAction}
        style={{
          marginTop: filled ? 6 : 0,
          width: '100%',
          height: filled ? 32 : 140,
          border: '1px dashed #d9d9d9',
          borderRadius: 8,
          background: '#fafafa',
          cursor: disabled ? 'not-allowed' : 'pointer',
          color: '#595959',
        }}
      >
        {action}
      </button>
    </div>
  )
}
