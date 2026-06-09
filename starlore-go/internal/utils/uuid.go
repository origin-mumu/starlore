package utils

import (
	"crypto/rand"
	"fmt"
	"time"
)

// GenerateUUID 生成符合 UUID v4 格式的随机 ID
func GenerateUUID() string {
	b := make([]byte, 16)
	_, err := rand.Read(b)
	if err != nil {
		// 回退到时间戳（极少触发）
		return fmt.Sprintf("%016x", time.Now().UnixNano())
	}
	// 设置版本号 (4) 和变体位
	b[6] = (b[6] & 0x0f) | 0x40
	b[8] = (b[8] & 0x3f) | 0x80
	return fmt.Sprintf("%08x-%04x-%04x-%04x-%012x",
		b[0:4], b[4:6], b[6:8], b[8:10], b[10:16])
}
