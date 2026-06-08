package utils

import (
	"fmt"

	"golang.org/x/crypto/bcrypt"
)

// HashPassword BCrypt 哈希密码
func HashPassword(raw string) (string, error) {
	bytes, err := bcrypt.GenerateFromPassword([]byte(raw), bcrypt.DefaultCost)
	if err != nil {
		return "", fmt.Errorf("failed to hash password: %w", err)
	}
	return string(bytes), nil
}

// VerifyPassword 校验密码
func VerifyPassword(raw, hash string) bool {
	return bcrypt.CompareHashAndPassword([]byte(hash), []byte(raw)) == nil
}
