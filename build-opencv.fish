#!/usr/bin/env fish

set options
set -a options (fish_opt -s t -l type --required-val)

argparse $options -- $argv

if set -q _flag_t
  set type $_flag_t
else
  set type Release
end

set flags

set -a flags -DCMAKE_BUILD_TYPE={$type} # -DCMAKE_INSTALL_PREFIX=/usr/local
set -a flags -DOPENCV_EXTRA_MODULES_PATH=../../modules/modules
set -a flags -DBUILD_JAVA=no -DBUILD_PYTHON=no -DWITH_CUDA=on -DWITH_CUDNN=off -DOPENCV_DNN_CUDA=OFF -DWITH_TBB=on -DBUILD_opencv_cudacodec=off -D WITH_CUBLAS=on -DCUDA_FAST_MATH=on -DENABLE_FAST_MATH=on
set -a flags -DCUDA_ARCH_BIN=11.8
# set -a flags -DOpen_BLAS_INCLUDE_SEARCH_PATHS=/usr/include/x86_64-linux-gnu -DOpen_BLAS_LIB_SEARCH_PATHS=/usr/lib/x86_64-linux-gnu
set -a flags -DCUDA_GENERATION=Auto

echo using $flags

env WITH_CUDA=on cmake .. $flags
