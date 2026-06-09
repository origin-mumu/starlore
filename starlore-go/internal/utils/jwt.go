package utils

import (
	"fmt"
	"time"

	"starlore-go/internal/config"

	"github.com/golang-jwt/jwt/v5"
)

// GenerateToken 生成 JWT token
func GenerateToken(id int, username string) (string, error) {
	claims := jwt.MapClaims{
		"sub":      username,
		"id":       id,
		"username": username,
		"exp":      time.Now().Add(time.Duration(config.JWT.Expiration) * time.Millisecond).Unix(),
		"iat":      time.Now().Unix(),
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString([]byte(config.JWT.Secret))
}

// ParseToken 解析 JWT token
func ParseToken(tokenStr string) (jwt.MapClaims, error) {
	token, err := jwt.Parse(tokenStr, func(t *jwt.Token) (interface{}, error) {
		if _, ok := t.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", t.Method.Alg())
		}
		return []byte(config.JWT.Secret), nil
	})
	if err != nil {
		return nil, fmt.Errorf("invalid token: %w", err)
	}
	claims, ok := token.Claims.(jwt.MapClaims)
	if !ok || !token.Valid {
		return nil, fmt.Errorf("invalid token claims")
	}
	return claims, nil
}
