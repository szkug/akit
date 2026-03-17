#pragma once

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct RSToolkit RSToolkit;

typedef struct RSTKRestriction {
    size_t startX;
    size_t endX;
    size_t startY;
    size_t endY;
} RSTKRestriction;

RSToolkit* rstk_create(int numberOfThreads);
void rstk_destroy(RSToolkit* toolkit);

void rstk_blend(RSToolkit* toolkit, int mode, const uint8_t* source, uint8_t* dest,
                size_t sizeX, size_t sizeY, const RSTKRestriction* restriction);

void rstk_blur(RSToolkit* toolkit, const uint8_t* input, uint8_t* output, size_t sizeX,
               size_t sizeY, size_t vectorSize, int radius,
               const RSTKRestriction* restriction);

void rstk_colorMatrix(RSToolkit* toolkit, const void* input, void* output, size_t inputVectorSize,
                      size_t outputVectorSize, size_t sizeX, size_t sizeY,
                      const float* matrix, const float* addVector,
                      const RSTKRestriction* restriction);

void rstk_convolve3x3(RSToolkit* toolkit, const void* input, void* output, size_t vectorSize,
                      size_t sizeX, size_t sizeY, const float* coefficients,
                      const RSTKRestriction* restriction);

void rstk_convolve5x5(RSToolkit* toolkit, const void* input, void* output, size_t vectorSize,
                      size_t sizeX, size_t sizeY, const float* coefficients,
                      const RSTKRestriction* restriction);

void rstk_histogram(RSToolkit* toolkit, const uint8_t* input, int32_t* output, size_t sizeX,
                    size_t sizeY, size_t vectorSize, const RSTKRestriction* restriction);

void rstk_histogramDot(RSToolkit* toolkit, const uint8_t* input, int32_t* output, size_t sizeX,
                       size_t sizeY, size_t vectorSize, const float* coefficients,
                       const RSTKRestriction* restriction);

void rstk_lut(RSToolkit* toolkit, const uint8_t* input, uint8_t* output, size_t sizeX,
              size_t sizeY, const uint8_t* red, const uint8_t* green, const uint8_t* blue,
              const uint8_t* alpha, const RSTKRestriction* restriction);

void rstk_lut3d(RSToolkit* toolkit, const uint8_t* input, uint8_t* output, size_t sizeX,
                size_t sizeY, const uint8_t* cube, size_t cubeSizeX, size_t cubeSizeY,
                size_t cubeSizeZ, const RSTKRestriction* restriction);

void rstk_resize(RSToolkit* toolkit, const uint8_t* input, uint8_t* output, size_t inputSizeX,
                 size_t inputSizeY, size_t vectorSize, size_t outputSizeX, size_t outputSizeY,
                 const RSTKRestriction* restriction);

void rstk_yuvToRgb(RSToolkit* toolkit, const uint8_t* input, uint8_t* output, size_t sizeX,
                   size_t sizeY, int format);

#ifdef __cplusplus
}
#endif
