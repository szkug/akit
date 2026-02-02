/*
 * C wrapper for RenderScriptToolkit to enable Kotlin/Native interop.
 */

#include "RenderScriptToolkitC.h"

#include "RenderScriptToolkit.h"

using renderscript::RenderScriptToolkit;
using renderscript::Restriction;

struct RSToolkit {
    RenderScriptToolkit impl;

    explicit RSToolkit(int numberOfThreads) : impl(numberOfThreads) {}
};

static inline const Restriction* toRestriction(const RSTKRestriction* restriction) {
    return reinterpret_cast<const Restriction*>(restriction);
}

RSToolkit* rstk_create(int numberOfThreads) {
    return new RSToolkit(numberOfThreads);
}

void rstk_destroy(RSToolkit* toolkit) {
    delete toolkit;
}

void rstk_blend(RSToolkit* toolkit, int mode, const uint8_t* source, uint8_t* dest,
                size_t sizeX, size_t sizeY, const RSTKRestriction* restriction) {
    toolkit->impl.blend(static_cast<RenderScriptToolkit::BlendingMode>(mode), source, dest,
                        sizeX, sizeY, toRestriction(restriction));
}

void rstk_blur(RSToolkit* toolkit, const uint8_t* input, uint8_t* output, size_t sizeX,
               size_t sizeY, size_t vectorSize, int radius,
               const RSTKRestriction* restriction) {
    toolkit->impl.blur(input, output, sizeX, sizeY, vectorSize, radius, toRestriction(restriction));
}

void rstk_colorMatrix(RSToolkit* toolkit, const void* input, void* output, size_t inputVectorSize,
                      size_t outputVectorSize, size_t sizeX, size_t sizeY,
                      const float* matrix, const float* addVector,
                      const RSTKRestriction* restriction) {
    toolkit->impl.colorMatrix(input, output, inputVectorSize, outputVectorSize, sizeX, sizeY,
                              matrix, addVector, toRestriction(restriction));
}

void rstk_convolve3x3(RSToolkit* toolkit, const void* input, void* output, size_t vectorSize,
                      size_t sizeX, size_t sizeY, const float* coefficients,
                      const RSTKRestriction* restriction) {
    toolkit->impl.convolve3x3(input, output, vectorSize, sizeX, sizeY, coefficients,
                              toRestriction(restriction));
}

void rstk_convolve5x5(RSToolkit* toolkit, const void* input, void* output, size_t vectorSize,
                      size_t sizeX, size_t sizeY, const float* coefficients,
                      const RSTKRestriction* restriction) {
    toolkit->impl.convolve5x5(input, output, vectorSize, sizeX, sizeY, coefficients,
                              toRestriction(restriction));
}

void rstk_histogram(RSToolkit* toolkit, const uint8_t* input, int32_t* output, size_t sizeX,
                    size_t sizeY, size_t vectorSize, const RSTKRestriction* restriction) {
    toolkit->impl.histogram(input, output, sizeX, sizeY, vectorSize, toRestriction(restriction));
}

void rstk_histogramDot(RSToolkit* toolkit, const uint8_t* input, int32_t* output, size_t sizeX,
                       size_t sizeY, size_t vectorSize, const float* coefficients,
                       const RSTKRestriction* restriction) {
    toolkit->impl.histogramDot(input, output, sizeX, sizeY, vectorSize, coefficients,
                               toRestriction(restriction));
}

void rstk_lut(RSToolkit* toolkit, const uint8_t* input, uint8_t* output, size_t sizeX,
              size_t sizeY, const uint8_t* red, const uint8_t* green, const uint8_t* blue,
              const uint8_t* alpha, const RSTKRestriction* restriction) {
    toolkit->impl.lut(input, output, sizeX, sizeY, red, green, blue, alpha,
                      toRestriction(restriction));
}

void rstk_lut3d(RSToolkit* toolkit, const uint8_t* input, uint8_t* output, size_t sizeX,
                size_t sizeY, const uint8_t* cube, size_t cubeSizeX, size_t cubeSizeY,
                size_t cubeSizeZ, const RSTKRestriction* restriction) {
    toolkit->impl.lut3d(input, output, sizeX, sizeY, cube, cubeSizeX, cubeSizeY, cubeSizeZ,
                        toRestriction(restriction));
}

void rstk_resize(RSToolkit* toolkit, const uint8_t* input, uint8_t* output, size_t inputSizeX,
                 size_t inputSizeY, size_t vectorSize, size_t outputSizeX, size_t outputSizeY,
                 const RSTKRestriction* restriction) {
    toolkit->impl.resize(input, output, inputSizeX, inputSizeY, vectorSize, outputSizeX,
                         outputSizeY, toRestriction(restriction));
}

void rstk_yuvToRgb(RSToolkit* toolkit, const uint8_t* input, uint8_t* output, size_t sizeX,
                   size_t sizeY, int format) {
    toolkit->impl.yuvToRgb(input, output, sizeX, sizeY,
                           static_cast<RenderScriptToolkit::YuvFormat>(format));
}
